package com.paymate.paymate_server.domain.attendance.service;

import com.paymate.paymate_server.domain.attendance.dto.AttendanceDto;
import com.paymate.paymate_server.domain.attendance.entity.Attendance;
import com.paymate.paymate_server.domain.attendance.enums.AttendanceStatus;
import com.paymate.paymate_server.domain.attendance.repository.AttendanceRepository;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.notification.enums.NotificationType;
import com.paymate.paymate_server.domain.notification.service.NotificationService; // 👈 서비스 Import 확인
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

    // ⚠️ NotificationRepository 제거됨
    private final NotificationService notificationService; // ✅ FCM 기능이 포함된 서비스 사용

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
        boolean isLate = false;

        // 지각 체크 로직
        Optional<Schedule> scheduleOpt = scheduleRepository.findByUserAndStoreAndWorkDate(
                user, store, now.toLocalDate()
        );

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

        // ✅ [수정됨] 1. 알바생에게 출근 완료 알림 (DB저장 + 푸시발송)
        notificationService.send(
                user,
                NotificationType.ATTENDANCE,
                "출근",
                "(" + now.format(DateTimeFormatter.ofPattern("HH:mm")) + ") 출근 체크 완료! 오늘도 기분 좋은 하루 보내세요."
        );

        // ✅ [수정됨] 2. 사장님에게 알림 (지각 여부 포함)
        String ownerMsg = isLate ?
                user.getName() + "님이 예정된 시간보다 늦게 출근했습니다. (" + now.format(DateTimeFormatter.ofPattern("HH:mm")) + ")" :
                user.getName() + "님이 출근했습니다. (" + now.format(DateTimeFormatter.ofPattern("HH:mm")) + ") 오늘도 힘찬 하루 되세요!";

        notificationService.send(
                store.getOwner(),
                NotificationType.ATTENDANCE,
                isLate ? "지각" : "출근",
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

        // ✅ [수정됨] 1. 알바생에게 퇴근 알림
        notificationService.send(
                attendance.getUser(),
                NotificationType.ATTENDANCE,
                "퇴근",
                "(" + now.format(DateTimeFormatter.ofPattern("HH:mm")) + ") 퇴근 체크 완료! 고생하셨습니다."
        );

        // ✅ [수정됨] 2. 사장님에게 퇴근 알림
        notificationService.send(
                attendance.getStore().getOwner(),
                NotificationType.ATTENDANCE,
                "퇴근",
                attendance.getUser().getName() + "님이 퇴근했습니다. 총 " + String.format("%.1f", totalHours) + "시간 근무."
        );

        return AttendanceDto.ClockOutResponse.builder()
                .success(true)
                .message("수고하셨습니다! 퇴근 처리됨.")
                .clockOutTime(attendance.getCheckOutTime())
                .totalHours(totalHours)
                .build();
    }

    // 3. 월간 조회 (기존 동일)
    @Transactional(readOnly = true)
    public List<AttendanceDto.AttendanceLog> getMonthlyLog(Long userId, int year, int month) {
        User user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1).minusSeconds(1);

        List<Attendance> list = attendanceRepository.findAllByUserAndCheckInTimeBetween(user, start, end);

        return list.stream().map(a -> AttendanceDto.AttendanceLog.builder()
                .attendanceId(a.getId())
                .workDate(a.getCheckInTime().toLocalDate().toString())
                .storeName(a.getStore().getName())
                .startTime(a.getCheckInTime())
                .endTime(a.getCheckOutTime())
                .status(a.getStatus().toString())
                .build()).collect(Collectors.toList());
    }

    // 4. 관리자 직접 수정 (기존 동일)
    public void modifyAttendance(Long attendanceId, AttendanceDto.ModifyRequest request) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("기록 없음"));

        LocalDate date = LocalDate.parse(request.getWorkDate());
        LocalTime start = LocalTime.parse(request.getStartTime());
        LocalTime end = LocalTime.parse(request.getEndTime());

        LocalDateTime startDateTime = LocalDateTime.of(date, start);
        LocalDateTime endDateTime = LocalDateTime.of(date, end);

        AttendanceStatus status = request.getStatus().equals("NORMAL") ? AttendanceStatus.OFF : AttendanceStatus.valueOf(request.getStatus());

        attendance.updateInfo(startDateTime, endDateTime, status);
    }

    // 5. 실시간 근무 현황 (기존 동일)
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
            totalWage += (long) (hours * 9860);
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

    // 6. 일별 근무 기록 (기존 동일)
    @Transactional(readOnly = true)
    public List<AttendanceDto.DailyLog> getDailyLog(Long storeId, String date) {
        Store store = storeRepository.findById(storeId).orElseThrow();
        List<Attendance> list = attendanceRepository.findAllByStoreAndWorkDate(store, date);

        return list.stream().map(a -> AttendanceDto.DailyLog.builder()
                .name(a.getUser().getName())
                .startTime(a.getCheckInTime() != null ? a.getCheckInTime().toLocalTime().toString() : "-")
                .endTime(a.getCheckOutTime() != null ? a.getCheckOutTime().toLocalTime().toString() : "-")
                .wage((long) (a.calculateTotalHours() * 9860))
                .build()).collect(Collectors.toList());
    }

    // 7. 수동 등록 (기존 동일)
    public Long manualRegister(AttendanceDto.ManualRegisterRequest request) {
        Store store = storeRepository.findById(request.getStoreId()).orElseThrow();
        User user = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        LocalDate date = LocalDate.parse(request.getWorkDate());
        LocalDateTime start = LocalDateTime.of(date, LocalTime.parse(request.getStartTime()));
        LocalDateTime end = LocalDateTime.of(date, LocalTime.parse(request.getEndTime()));

        Attendance attendance = Attendance.builder()
                .store(store)
                .user(user)
                .workDate(request.getWorkDate())
                .checkInTime(start)
                .checkOutTime(end)
                .status(AttendanceStatus.OFF)
                .build();

        return attendanceRepository.save(attendance).getId();
    }

    // 8. 정정 요청 반영 (기존 동일)
    @Transactional
    public void updateByRequest(Long attendanceId, String afterValue) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance not found: " + attendanceId));

        String[] times = afterValue.split("~");
        if (times.length != 2) {
            throw new IllegalArgumentException("Invalid time format (must be Start~End): " + afterValue);
        }

        LocalTime startTime = LocalTime.parse(times[0].trim());
        LocalTime endTime = LocalTime.parse(times[1].trim());

        LocalDate date = LocalDate.parse(attendance.getWorkDate());
        LocalDateTime newStart = LocalDateTime.of(date, startTime);
        LocalDateTime newEnd = LocalDateTime.of(date, endTime);

        attendance.updateInfo(newStart, newEnd, AttendanceStatus.OFF);

        System.out.println("✅ [AttendanceService] 정정 요청에 의해 데이터 수정 완료 ID: " + attendanceId);
    }
}