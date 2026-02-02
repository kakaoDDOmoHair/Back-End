package com.paymate.paymate_server.domain.attendance.service;

import com.paymate.paymate_server.domain.attendance.dto.AttendanceDto;
import com.paymate.paymate_server.domain.attendance.entity.Attendance;
import com.paymate.paymate_server.domain.attendance.enums.AttendanceStatus;
import com.paymate.paymate_server.domain.attendance.repository.AttendanceRepository;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.notification.enums.NotificationType;
import com.paymate.paymate_server.domain.notification.service.NotificationService;
import com.paymate.paymate_server.domain.schedule.entity.Schedule;
import com.paymate.paymate_server.domain.schedule.repository.ScheduleRepository;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;
    private final ScheduleRepository scheduleRepository;

    private final NotificationService notificationService;

    /**
     * DB 시각을 KST ISO 8601 문자열로 내려줌 (예: "2025-01-31T10:21:00+09:00").
     * 저장 시각은 서버 기본 타임존(UTC 등)일 수 있으므로, 서버 시각 → KST로 변환해 표시.
     * (서버가 UTC면 01:21 저장 → 10:21 KST로 내려줘서 "기록 시간"이 10:21로 보이게 함)
     */
    private static String toIsoKst(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /** 표시용 "HH:mm~HH:mm" 또는 "HH:mm~" (기록 시간 컬럼용). DB 시각을 KST로 해석 후 포맷. 퇴근 전이면 "HH:mm~" */
    private static String formatTimeDisplay(LocalDateTime start, LocalDateTime end) {
        if (start == null) return "";
        ZoneId kst = ZoneId.of("Asia/Seoul");
        String startStr = start.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(kst)
                .toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        if (end == null) return startStr + "~";
        String endStr = end.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(kst)
                .toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        return startStr + "~" + endStr;
    }

    // 1. 출근 (Clock-In)
    public AttendanceDto.ClockInResponse clockIn(AttendanceDto.ClockInRequest request) {
        User user = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("매장 없음"));

        if (attendanceRepository.existsByUserAndStatus(user, AttendanceStatus.ON)) {
            throw new IllegalStateException("이미 출근 처리된 상태입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        AttendanceStatus status = AttendanceStatus.ON;

        // 지각 체크: 한국 시간 기준 날짜·시각 사용 (서버 타임존과 무관하게 스케줄/비교 일치)
        ZoneId korea = ZoneId.of("Asia/Seoul");
        LocalDate workDateKorea = LocalDate.now(korea);
        LocalTime actualStartKorea = ZonedDateTime.now(korea).toLocalTime();

        Optional<Schedule> scheduleOpt = scheduleRepository.findByUserAndStoreAndWorkDate(
                user, store, workDateKorea
        );

        boolean isLate = false;
        if (scheduleOpt.isPresent()) {
            Schedule schedule = scheduleOpt.get();
            LocalTime scheduledStart = schedule.getStartTime();
            if (actualStartKorea.isAfter(scheduledStart)) {
                status = AttendanceStatus.LATE;
                isLate = true;
            }
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .store(store)
                .checkInTime(now)
                .workDate(workDateKorea.toString())
                .status(status)
                .lat(request.getLat())
                .lon(request.getLon())
                .wifiBssid(request.getWifiBssid())
                .build();

        attendanceRepository.save(attendance);

        // ▼▼▼ [추가] 알바생에게 알림 발송 ▼▼▼
        notificationService.send(
                user,
                NotificationType.ATTENDANCE,
                "출근",
                "(" + now.format(DateTimeFormatter.ofPattern("HH:mm")) + ") 출근 체크 완료! 오늘도 화이팅하세요!"
        );

        // ▼▼▼ [추가] 사장님에게 알림 발송 ▼▼▼
        String ownerMsg = isLate ?
                user.getName() + "님이 지각했습니다. (" + now.format(DateTimeFormatter.ofPattern("HH:mm")) + ")" :
                user.getName() + "님이 출근했습니다. (" + now.format(DateTimeFormatter.ofPattern("HH:mm")) + ")";

        notificationService.send(
                store.getOwner(),
                NotificationType.ATTENDANCE,
                isLate ? "지각 알림" : "출근 알림",
                ownerMsg
        );

        return AttendanceDto.ClockInResponse.builder()
                .success(true)
                .attendanceId(attendance.getId())
                .status(status.toString())
                .clockInTime(attendance.getCheckInTime())
                .build();
    }

    // 2. 퇴근 (Clock-Out)
    public AttendanceDto.ClockOutResponse clockOut(AttendanceDto.ClockOutRequest request) {
        Attendance attendance = attendanceRepository.findById(request.getAttendanceId())
                .orElseThrow(() -> new IllegalArgumentException("기록이 존재하지 않습니다."));

        LocalDateTime now = LocalDateTime.now();
        attendance.clockOut(now, request.getLat(), request.getLon());

        double totalHours = attendance.calculateTotalHours();

        // ▼▼▼ [추가] 퇴근 알림 발송 ▼▼▼
        notificationService.send(
                attendance.getUser(),
                NotificationType.ATTENDANCE,
                "퇴근",
                "퇴근 처리가 완료되었습니다. 고생하셨습니다! (근무시간: " + String.format("%.1f", totalHours) + "시간)"
        );

        notificationService.send(
                attendance.getStore().getOwner(),
                NotificationType.ATTENDANCE,
                "퇴근 알림",
                attendance.getUser().getName() + "님이 퇴근했습니다."
        );

        return AttendanceDto.ClockOutResponse.builder()
                .success(true)
                .message("수고하셨습니다! 퇴근 처리됨.")
                .clockOutTime(attendance.getCheckOutTime())
                .totalHours(totalHours)
                .build();
    }

    // 3. 월간 조회 — 해당 유저만, 최신순 정렬, (workDate, checkInTime) 기준 중복 제거
    @Transactional(readOnly = true)
    public List<AttendanceDto.AttendanceLog> getMonthlyLog(Long userId, int year, int month) {
        User user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1).minusSeconds(1);

        List<Attendance> list = attendanceRepository.findAllByUserAndCheckInTimeBetweenOrderByCheckInTimeDesc(user, start, end);

        // 같은 날짜·같은 출근 시각 기준 중복 제거 (첫 번째 = 최신 id 유지)
        Map<String, Attendance> byKey = new LinkedHashMap<>();
        for (Attendance a : list) {
            String key = (a.getWorkDate() != null ? a.getWorkDate() : a.getCheckInTime().toLocalDate().toString())
                    + "|" + (a.getCheckInTime() != null ? a.getCheckInTime().toString() : "");
            byKey.putIfAbsent(key, a);
        }
        List<Attendance> deduped = new ArrayList<>(byKey.values());

        return deduped.stream().map(a -> AttendanceDto.AttendanceLog.builder()
                .attendanceId(a.getId())
                .userId(a.getUser() != null ? a.getUser().getId() : null)
                .name(a.getUser() != null ? a.getUser().getName() : null)
                .workDate(a.getCheckInTime() != null ? a.getCheckInTime().toLocalDate().toString() : a.getWorkDate())
                .storeName(a.getStore().getName())
                .startTime(toIsoKst(a.getCheckInTime()))
                .endTime(toIsoKst(a.getCheckOutTime()))
                .time(formatTimeDisplay(a.getCheckInTime(), a.getCheckOutTime()))
                .status(a.getStatus().toString())
                .build()).collect(Collectors.toList());
    }

    // 3-1. 현재 출근 중인 기록 1건 조회 (퇴근 전 attendanceId 복구용)
    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentOpenAttendance(Long userId) {
        User user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        Optional<Attendance> on = attendanceRepository.findByUserAndStatus(user, AttendanceStatus.ON);
        if (on.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("attendanceId", on.get().getId());
            result.put("status", "ON");
            return result;
        }
        Optional<Attendance> late = attendanceRepository.findByUserAndStatus(user, AttendanceStatus.LATE);
        if (late.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("attendanceId", late.get().getId());
            result.put("status", "LATE");
            return result;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("attendanceId", null);
        result.put("message", "출근 중인 기록이 없습니다.");
        return result;
    }

    // 4. 관리자 수정 (기존 Develop 코드 유지)
    public void modifyAttendance(Long attendanceId, AttendanceDto.ModifyRequest request) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("기록 없음"));

        LocalDate date = LocalDate.parse(request.getWorkDate());
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalTime endTime = LocalTime.parse(request.getEndTime());

        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime endDateTime;
        
        // endTime이 startTime보다 작으면 다음날로 처리 (야간 근무)
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            endDateTime = LocalDateTime.of(date.plusDays(1), endTime);
        } else {
            endDateTime = LocalDateTime.of(date, endTime);
        }

        AttendanceStatus status = request.getStatus().equals("NORMAL") ? AttendanceStatus.OFF : AttendanceStatus.valueOf(request.getStatus());

        attendance.updateInfo(startDateTime, endDateTime, status);
    }

    // 5. 실시간 근무 현황 (기존 Develop 코드 유지)
    @Transactional(readOnly = true)
    public AttendanceDto.TodayResponse getTodayStatus(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("매장 없음"));
        String today = LocalDate.now().toString();
        List<Attendance> list = attendanceRepository.findAllByStoreAndWorkDate(store, today);

        double totalTime = 0.0;
        long totalWage = 0;

        List<AttendanceDto.AttendanceLog> logs = list.stream().map(a -> AttendanceDto.AttendanceLog.builder()
                .attendanceId(a.getId())
                .userId(a.getUser() != null ? a.getUser().getId() : null)
                .name(a.getUser() != null ? a.getUser().getName() : null)
                .workDate(a.getWorkDate())
                .storeName(a.getStore().getName())
                .startTime(toIsoKst(a.getCheckInTime()))
                .endTime(toIsoKst(a.getCheckOutTime()))
                .time(formatTimeDisplay(a.getCheckInTime(), a.getCheckOutTime()))
                .status(a.getStatus().toString())
                .build()).collect(Collectors.toList());

        for (Attendance a : list) {
            double hours = a.calculateTotalHours();
            totalTime += hours;
            totalWage += (long) (hours * 10320);
        }

        Map<String, Double> summary = new HashMap<>();
        summary.put(today, totalTime);

        return AttendanceDto.TodayResponse.builder()
                .totalTime(totalTime)
                .totalWage(totalWage)
                .summary(summary)
                .list(logs)
                .build();
    }

    // 6. 일별 조회 (기존 Develop 코드 유지)
    @Transactional(readOnly = true)
    public List<AttendanceDto.DailyLog> getDailyLog(Long storeId, String date) {
        Store store = storeRepository.findById(storeId).orElseThrow();
        List<Attendance> list = attendanceRepository.findAllByStoreAndWorkDate(store, date);

        return list.stream().map(a -> AttendanceDto.DailyLog.builder()
                .attendanceId(a.getId())
                .userId(a.getUser() != null ? a.getUser().getId() : null)
                .name(a.getUser() != null ? a.getUser().getName() : null)
                .startTime(toIsoKst(a.getCheckInTime()))
                .endTime(toIsoKst(a.getCheckOutTime()))
                .time(formatTimeDisplay(a.getCheckInTime(), a.getCheckOutTime()))
                .wage((long) (a.calculateTotalHours() * 10320))
                .status(a.getStatus() != null ? a.getStatus().toString() : null)
                .build()).collect(Collectors.toList());
    }

    // 7. 수동 등록 (기존 Develop 코드 유지)
    public Long manualRegister(AttendanceDto.ManualRegisterRequest request) {
        Store store = storeRepository.findById(request.getStoreId()).orElseThrow();
        User user = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저(ID:" + request.getUserId() + ")가 없습니다."));

        LocalDate date = LocalDate.parse(request.getWorkDate());
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalTime endTime = LocalTime.parse(request.getEndTime());
        
        LocalDateTime start = LocalDateTime.of(date, startTime);
        LocalDateTime end;
        
        // endTime이 startTime보다 작으면 다음날로 처리 (야간 근무)
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            end = LocalDateTime.of(date.plusDays(1), endTime);
        } else {
            end = LocalDateTime.of(date, endTime);
        }

        Attendance attendance = Attendance.builder()
                .store(store)
                .user(user)
                .workDate(request.getWorkDate())
                .checkInTime(start)
                .checkOutTime(end)
                .status(AttendanceStatus.PENDING)
                .build();

        return attendanceRepository.save(attendance).getId();
    }

    // 8. 정정 요청 처리 (기존 Develop 코드 유지)
    @Transactional
    public void updateAttendance(Long attendanceId, String newValue) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 근무 기록이 없습니다. ID=" + attendanceId));

        try {
            LocalTime time = LocalTime.parse(newValue);
            attendance.updateEndTime(time);
        } catch (Exception e) {
            throw new IllegalArgumentException("시간 형식이 올바르지 않습니다. (예: 09:00) 입력값: " + newValue);
        }
    }
    // ✅ 여기에 붙여넣으세요!
    @Transactional
    public void updateByRequest(Long attendanceId, String afterValue) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 근태 기록이 없습니다. ID=" + attendanceId));

        if (afterValue == null || afterValue.isBlank()) {
            throw new IllegalArgumentException("afterValue가 비어 있습니다. (예: 09:00~18:00)");
        }

        // afterValue: "HH:mm~HH:mm"
        String[] times = afterValue.split("~");
        if (times.length != 2) {
            throw new IllegalArgumentException("시간 형식이 올바르지 않습니다. (예: 09:00~18:00)");
        }

        LocalTime newStart = LocalTime.parse(times[0].trim());
        LocalTime newEnd = LocalTime.parse(times[1].trim());

        LocalDate baseDate;
        if (attendance.getWorkDate() != null && !attendance.getWorkDate().isBlank()) {
            baseDate = LocalDate.parse(attendance.getWorkDate());
        } else if (attendance.getCheckInTime() != null) {
            baseDate = attendance.getCheckInTime().toLocalDate();
        } else if (attendance.getCheckOutTime() != null) {
            baseDate = attendance.getCheckOutTime().toLocalDate();
        } else {
            baseDate = LocalDate.now();
        }

        LocalDateTime newCheckIn = LocalDateTime.of(baseDate, newStart);
        LocalDateTime newCheckOut = (newEnd.isBefore(newStart) || newEnd.equals(newStart))
                ? LocalDateTime.of(baseDate.plusDays(1), newEnd)
                : LocalDateTime.of(baseDate, newEnd);

        attendance.updateTimes(newCheckIn, newCheckOut);
        log.info("✅ [AttendanceService] 근태 정정 완료! ID: {}, 변경시간: {} ~ {}", attendanceId, newStart, newEnd);
    }

    /** 정정 요청 승인(DELETE) 시 근태 기록 삭제 */
    @Transactional
    public void deleteByRequest(Long attendanceId) {
        if (attendanceId == null) throw new IllegalArgumentException("attendanceId가 비어 있습니다.");
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new IllegalArgumentException("해당 근태 기록이 없습니다. ID=" + attendanceId);
        }
        attendanceRepository.deleteById(attendanceId);
        log.info("🗑️ [AttendanceService] 근태 삭제 완료! ID: {}", attendanceId);
    }
}