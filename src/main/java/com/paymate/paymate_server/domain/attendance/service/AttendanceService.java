package com.paymate.paymate_server.domain.attendance.service;

import com.paymate.paymate_server.domain.attendance.dto.AttendanceDto;
import com.paymate.paymate_server.domain.attendance.entity.Attendance;
import com.paymate.paymate_server.domain.attendance.entity.AttendanceRequest; // [New]
import com.paymate.paymate_server.domain.attendance.enums.AttendanceRequestStatus; // [New]
import com.paymate.paymate_server.domain.attendance.enums.AttendanceRequestType; // [New]
import com.paymate.paymate_server.domain.attendance.enums.AttendanceStatus;
import com.paymate.paymate_server.domain.attendance.repository.AttendanceRepository;
import com.paymate.paymate_server.domain.attendance.repository.AttendanceRequestRepository; // [New]
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
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
    private final AttendanceRequestRepository attendanceRequestRepository; // [New] 추가됨

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

        if (scheduleOpt.isPresent()) {
            Schedule schedule = scheduleOpt.get();
            LocalTime scheduledStart = schedule.getStartTime();
            LocalTime actualStart = now.toLocalTime();

            if (actualStart.isAfter(scheduledStart)) {
                status = AttendanceStatus.LATE;
            }
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .store(store)
                .checkInTime(now)
                .workDate(now.toLocalDate().toString()) // 날짜 필드 채우기 (조회용)
                .status(status)
                .lat(request.getLat())
                .lon(request.getLon())
                .wifiBssid(request.getWifiBssid())
                .build();

        attendanceRepository.save(attendance);

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

        attendance.clockOut(LocalDateTime.now(), request.getLat(), request.getLon());

        return AttendanceDto.ClockOutResponse.builder()
                .success(true)
                .message("수고하셨습니다! 퇴근 처리됨.")
                .clockOutTime(attendance.getCheckOutTime())
                .totalHours(attendance.calculateTotalHours())
                .build();
    }

    // 3. 월간 조회 (알바생용)
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

    // 4. 관리자 직접 수정 (Manager Modify)
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

    // =========================================================================
    // ▼ [NEW] 아래부터 새로 추가된 메서드들입니다.
    // =========================================================================

    // 5. 실시간 근무 현황 조회 (Today) - 사장님용
    @Transactional(readOnly = true)
    public AttendanceDto.TodayResponse getTodayStatus(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("매장 없음"));

        // 오늘 날짜 String 구하기 (YYYY-MM-DD)
        String today = LocalDate.now().toString();

        // [주의] AttendanceRepository에 findAllByStoreAndWorkDate 메서드가 있어야 합니다.
        List<Attendance> list = attendanceRepository.findAllByStoreAndWorkDate(store, today);

        double totalTime = 0.0;
        long totalWage = 0;

        // 리스트 변환
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

        // 통계 계산
        for (Attendance a : list) {
            double hours = a.calculateTotalHours();
            totalTime += hours;
            totalWage += (long) (hours * 9860); // 임시 시급 (9860원)
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

    // 6. 일별 근무 기록 조회 (Daily)
    @Transactional(readOnly = true)
    public List<AttendanceDto.DailyLog> getDailyLog(Long storeId, String date) {
        Store store = storeRepository.findById(storeId).orElseThrow();
        // Repository 메서드 필요
        List<Attendance> list = attendanceRepository.findAllByStoreAndWorkDate(store, date);

        return list.stream().map(a -> AttendanceDto.DailyLog.builder()
                .name(a.getUser().getName())
                .startTime(a.getCheckInTime() != null ? a.getCheckInTime().toLocalTime().toString() : "-")
                .endTime(a.getCheckOutTime() != null ? a.getCheckOutTime().toLocalTime().toString() : "-")
                .wage((long) (a.calculateTotalHours() * 9860))
                .build()).collect(Collectors.toList());
    }

    // 7. 근무 기록 직접 등록 (Manual Register)
    public Long manualRegister(AttendanceDto.ManualRegisterRequest request) {
        Store store = storeRepository.findById(request.getStoreId()).orElseThrow();

        // [주의] 실제로는 Request에 userId가 포함되어야 합니다.
        // 테스트를 위해 임시로 ID가 2인 유저(알바생)로 고정합니다. 테스트 시 본인 DB에 맞는 ID로 수정하세요.
        User user = memberRepository.findById(2L)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저(ID:2)가 없습니다."));

        LocalDate date = LocalDate.parse(request.getWorkDate());
        LocalDateTime start = LocalDateTime.of(date, LocalTime.parse(request.getStartTime()));
        LocalDateTime end = LocalDateTime.of(date, LocalTime.parse(request.getEndTime()));

        Attendance attendance = Attendance.builder()
                .store(store)
                .user(user)
                .workDate(request.getWorkDate())
                .checkInTime(start)
                .checkOutTime(end)
                .status(AttendanceStatus.PENDING) // 승인 대기 상태로 등록
                .build();

        return attendanceRepository.save(attendance).getId();
    }

    // 8. 근무 기록 수정 요청 (Request Correction)
    public Long requestCorrection(AttendanceDto.CorrectionRequest request) {
        Attendance attendance = attendanceRepository.findById(request.getAttendanceId())
                .orElseThrow(() -> new IllegalArgumentException("기록 없음"));

        // "17:00 ~ 22:30" 형식 파싱
        String[] times = request.getAfterTime().split("~");
        LocalTime start = LocalTime.parse(times[0].trim());
        LocalTime end = LocalTime.parse(times[1].trim());

        AttendanceRequest req = AttendanceRequest.builder()
                .attendance(attendance)
                .user(attendance.getUser())
                .targetDate(LocalDate.parse(request.getTargetDate()))
                .requestType(AttendanceRequestType.MODIFICATION)
                .status(AttendanceRequestStatus.PENDING)
                .beforeTime(request.getBeforeTime())
                .afterTime(request.getAfterTime())
                .reason(request.getReason())
                .targetStartTime(start)
                .targetEndTime(end)
                .build();

        return attendanceRequestRepository.save(req).getId();
    }

    // 9. 요청 처리 (승인/거절)
    public void processRequest(Long requestId, String statusStr) {
        AttendanceRequest req = attendanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("요청 없음"));

        AttendanceRequestStatus status = AttendanceRequestStatus.valueOf(statusStr);
        req.updateStatus(status); // 요청 상태 업데이트

        // 승인(APPROVED)인 경우 -> 실제 Attendance 데이터 수정
        if (status == AttendanceRequestStatus.APPROVED) {
            Attendance attendance = req.getAttendance();
            LocalDateTime newStart = LocalDateTime.of(req.getTargetDate(), req.getTargetStartTime());
            LocalDateTime newEnd = LocalDateTime.of(req.getTargetDate(), req.getTargetEndTime());

            // 상태를 OFF(정상 퇴근)로 변경하며 시간 업데이트
            attendance.updateInfo(newStart, newEnd, AttendanceStatus.OFF);
        }
    }
}