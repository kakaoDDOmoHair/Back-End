package com.paymate.paymate_server.domain.attendance.service;

import com.paymate.paymate_server.domain.attendance.dto.AttendanceDto;
import com.paymate.paymate_server.domain.attendance.entity.Attendance;
import com.paymate.paymate_server.domain.attendance.enums.AttendanceStatus;
import com.paymate.paymate_server.domain.attendance.repository.AttendanceRepository;
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

    /**
     * 1. 출근 (Clock-In)
     */
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

        // 지각 체크
        Optional<Schedule> scheduleOpt = scheduleRepository.findByUserAndStoreAndWorkDate(
                user, store, now.toLocalDate()
        );

        if (scheduleOpt.isPresent()) {
            Schedule schedule = scheduleOpt.get();
            if (now.toLocalTime().isAfter(schedule.getStartTime())) {
                status = AttendanceStatus.LATE;
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

        return AttendanceDto.ClockInResponse.builder()
                .success(true)
                .attendanceId(attendance.getId())
                .status(status.toString())
                .clockInTime(attendance.getCheckInTime())
                .build();
    }

    /**
     * 2. 퇴근 (Clock-Out)
     */
    public AttendanceDto.ClockOutResponse clockOut(AttendanceDto.ClockOutRequest request) {
        Attendance attendance = attendanceRepository.findById(request.getAttendanceId())
                .orElseThrow(() -> new IllegalArgumentException("기록이 존재하지 않습니다."));

        attendance.clockOut(LocalDateTime.now(), request.getLat(), request.getLon());

        return AttendanceDto.ClockOutResponse.builder()
                .success(true)
                .message("수고하셨습니다!")
                .clockOutTime(attendance.getCheckOutTime())
                .totalHours(attendance.calculateTotalHours()) // 자동 휴게시간 차감 로직 적용됨
                .build();
    }

    /**
     * 3. 월간 조회 (알바생용)
     */
    @Transactional(readOnly = true)
    public List<AttendanceDto.AttendanceLog> getMonthlyLog(Long userId, int year, int month) {
        User user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1).minusSeconds(1);

        List<Attendance> list = attendanceRepository.findAllByUserAndCheckInTimeBetween(user, start, end);

        return list.stream().map(a -> AttendanceDto.AttendanceLog.builder()
                .attendanceId(a.getId())
                .workDate(a.getWorkDate())
                .storeName(a.getStore().getName())
                .startTime(a.getCheckInTime())
                .endTime(a.getCheckOutTime())
                .status(a.getStatus().toString())
                .build()).collect(Collectors.toList());
    }

    /**
     * 4. 관리자 직접 수정 (Manager Modify)
     */
    public void modifyAttendance(Long attendanceId, AttendanceDto.ModifyRequest request) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("기록 없음"));

        LocalDate date = LocalDate.parse(request.getWorkDate());
        LocalDateTime startDateTime = LocalDateTime.of(date, LocalTime.parse(request.getStartTime()));
        LocalDateTime endDateTime = LocalDateTime.of(date, LocalTime.parse(request.getEndTime()));

        AttendanceStatus status = request.getStatus().equals("NORMAL") ? AttendanceStatus.OFF : AttendanceStatus.valueOf(request.getStatus());

        attendance.updateInfo(startDateTime, endDateTime, status);
    }

    /**
     * 5. 실시간 근무 현황 조회 (Today) - 사장님용
     */
    @Transactional(readOnly = true)
    public AttendanceDto.TodayResponse getTodayStatus(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("매장 없음"));

        String today = LocalDate.now().toString();
        List<Attendance> list = attendanceRepository.findAllByStoreAndWorkDate(store, today);

        double totalTime = 0.0;
        long totalWage = 0;

        List<AttendanceDto.AttendanceLog> logs = list.stream().map(a ->
                AttendanceDto.AttendanceLog.builder()
                        .attendanceId(a.getId())
                        .workDate(a.getWorkDate())
                        .storeName(a.getStore().getName())
                        .startTime(a.getCheckInTime())
                        .endTime(a.getCheckOutTime())
                        .status(a.getStatus().toString())
                        .build()
        ).collect(Collectors.toList());

        for (Attendance a : list) {
            double hours = a.calculateTotalHours();
            totalTime += hours;

            // 유저별 실제 설정된 시급 적용
            int hourlyWage = (a.getUser().getHourlyWage() != null && a.getUser().getHourlyWage() > 0)
                    ? a.getUser().getHourlyWage() : 9860;

            totalWage += (long) (hours * hourlyWage);
        }

        Map<String, Double> summary = new HashMap<>();
        summary.put(today, Math.round(totalTime * 10.0) / 10.0);

        return AttendanceDto.TodayResponse.builder()
                .totalTime(Math.round(totalTime * 10.0) / 10.0)
                .totalWage(totalWage)
                .summary(summary)
                .list(logs)
                .build();
    }

    /**
     * 6. 일별 근무 기록 조회 (Daily)
     */
    @Transactional(readOnly = true)
    public List<AttendanceDto.DailyLog> getDailyLog(Long storeId, String date) {
        Store store = storeRepository.findById(storeId).orElseThrow();
        List<Attendance> list = attendanceRepository.findAllByStoreAndWorkDate(store, date);

        return list.stream().map(a -> {
            int hourlyWage = (a.getUser().getHourlyWage() != null && a.getUser().getHourlyWage() > 0)
                    ? a.getUser().getHourlyWage() : 9860;

            return AttendanceDto.DailyLog.builder()
                    .name(a.getUser().getName())
                    .startTime(a.getCheckInTime() != null ? a.getCheckInTime().toLocalTime().toString() : "-")
                    .endTime(a.getCheckOutTime() != null ? a.getCheckOutTime().toLocalTime().toString() : "-")
                    .wage((long) (a.calculateTotalHours() * hourlyWage))
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 7. 근무 기록 직접 등록 (Manual Register)
     */
    public Long manualRegister(AttendanceDto.ManualRegisterRequest request) {
        Store store = storeRepository.findById(request.getStoreId()).orElseThrow();
        User user = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다."));

        LocalDate date = LocalDate.parse(request.getWorkDate());
        LocalDateTime start = LocalDateTime.of(date, LocalTime.parse(request.getStartTime()));
        LocalDateTime end = LocalDateTime.of(date, LocalTime.parse(request.getEndTime()));

        Attendance attendance = Attendance.builder()
                .store(store)
                .user(user)
                .workDate(request.getWorkDate())
                .checkInTime(start)
                .checkOutTime(end)
                .status(AttendanceStatus.PENDING) // 관리자 승인 전 대기 상태
                .build();

        return attendanceRepository.save(attendance).getId();
    }

    /**
     * 8. 근무 시간 수정 (퇴근 시간 정정)
     */
    @Transactional
    public void updateAttendance(Long attendanceId, String newValue) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("기록 없음"));

        try {
            LocalTime time = LocalTime.parse(newValue);
            attendance.updateEndTime(time);
        } catch (Exception e) {
            throw new IllegalArgumentException("시간 형식이 올바르지 않습니다. (예: 09:00)");
        }
    }
}