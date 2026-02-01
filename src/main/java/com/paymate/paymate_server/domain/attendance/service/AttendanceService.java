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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;
    private final ScheduleRepository scheduleRepository;

    // [추가] 알림 서비스 주입
    private final NotificationService notificationService;

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

        // 지각 체크 로직
        Optional<Schedule> scheduleOpt = scheduleRepository.findByUserAndStoreAndWorkDate(
                user, store, now.toLocalDate()
        );

        boolean isLate = false;
        if (scheduleOpt.isPresent()) {
            Schedule schedule = scheduleOpt.get();
            LocalTime scheduledStart = schedule.getStartTime();
            LocalTime actualStart = now.toLocalTime();

            if (actualStart.isAfter(scheduledStart)) {
                status = AttendanceStatus.LATE;
                isLate = true;
            }
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .store(store)
                .checkInTime(now)
                .workDate(now.toLocalDate().toString())
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

    // 3. 월간 조회 (기존 Develop 코드 유지)
    @Transactional(readOnly = true)
    public List<AttendanceDto.AttendanceLog> getMonthlyLog(Long userId, int year, int month) {
        User user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1).minusSeconds(1);

        List<Attendance> list = attendanceRepository.findAllByUserAndCheckInTimeBetween(user, start, end);

        return list.stream().map(a -> AttendanceDto.AttendanceLog.builder()
                .attendanceId(a.getId())
                .userId(a.getUser() != null ? a.getUser().getId() : null)
                .name(a.getUser() != null ? a.getUser().getName() : null)
                .workDate(a.getCheckInTime().toLocalDate().toString())
                .storeName(a.getStore().getName())
                .startTime(a.getCheckInTime())
                .endTime(a.getCheckOutTime())
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

        List<AttendanceDto.AttendanceLog> logs = list.stream().map(a -> {
            return AttendanceDto.AttendanceLog.builder()
                    .attendanceId(a.getId())
                    .userId(a.getUser() != null ? a.getUser().getId() : null)
                    .name(a.getUser() != null ? a.getUser().getName() : null)
                    .workDate(a.getWorkDate())
                    .storeName(a.getStore().getName())
                    .startTime(a.getCheckInTime())
                    .endTime(a.getCheckOutTime())
                    .status(a.getStatus().toString())
                    .build();
        }).collect(Collectors.toList());

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
                .startTime(a.getCheckInTime() != null ? a.getCheckInTime().toLocalTime().toString() : "-")
                .endTime(a.getCheckOutTime() != null ? a.getCheckOutTime().toLocalTime().toString() : "-")
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
        // 1. 수정할 근태 기록 찾기
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 근태 기록이 없습니다. ID=" + attendanceId));

        // 2. 값 변경 로직 (String -> 시간/데이터 변환 필요)
        // 예시: LocalDateTime parsedTime = LocalDateTime.parse(afterValue);
        // attendance.updateTime(parsedTime);

        System.out.println("근태 기록 수정 완료: " + attendanceId + " -> " + afterValue);
    }
}