package com.paymate.paymate_server.domain.schedule.service;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.notification.enums.NotificationType;
import com.paymate.paymate_server.domain.notification.service.NotificationService; // 👈 서비스 Import
import com.paymate.paymate_server.domain.schedule.dto.ScheduleDto;
import com.paymate.paymate_server.domain.schedule.entity.Schedule;
import com.paymate.paymate_server.domain.schedule.repository.ScheduleRepository;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;
    // NotificationRepository 제거됨
    private final NotificationService notificationService; // 👈 알림 서비스(FCM 포함) 사용

    // 1. 근무 스케줄 등록 (사장님이 배정 시 알림 발송)
    @Transactional
    public ScheduleDto.CreateResponse createSchedule(ScheduleDto.CreateRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("매장 없음"));
        User worker = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        Schedule schedule = Schedule.builder()
                .store(store)
                .user(worker)
                .workDate(request.getWorkDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        scheduleRepository.save(schedule);

        // 🔔 [수정됨] 스케줄 배정 알림 (DB저장 + 푸시발송)
        notificationService.send(
                worker,
                NotificationType.WORK,
                "새로운 스케줄 배정 📅",
                String.format("[%s] %s 근무 스케줄이 등록되었습니다.", store.getName(), request.getWorkDate())
        );

        return ScheduleDto.CreateResponse.builder()
                .scheduleId(schedule.getId())
                .status("ASSIGNED")
                .build();
    }

    // 2. 월간 스케줄 조회
    public List<ScheduleDto.MonthlyResponse> getMonthlySchedule(Long storeId, int year, int month) {
        return scheduleRepository.findMonthlySchedule(storeId, year, month).stream()
                .map(s -> ScheduleDto.MonthlyResponse.builder()
                        .date(s.getWorkDate().toString())
                        .userId(s.getUser().getId())
                        .name(s.getUser().getName())
                        .time(s.getStartTime() + "~" + s.getEndTime())
                        .build())
                .collect(Collectors.toList());
    }

    // 5. 주간 근무 시간표 조회 (사장님용)
    public List<ScheduleDto.WeeklyResponse> getWeeklySchedule(Long storeId, LocalDate startDate) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("매장 없음"));

        LocalDate endDate = startDate.plusDays(6);

        List<Schedule> schedules = scheduleRepository.findByStoreAndWorkDateBetween(store, startDate, endDate);

        List<ScheduleDto.WeeklyResponse> response = new ArrayList<>();

        Map<LocalDate, List<Schedule>> byDate = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getWorkDate));

        for (LocalDate date = startDate; date.isBefore(endDate.plusDays(1)); date = date.plusDays(1)) {
            List<Schedule> dailySchedules = byDate.getOrDefault(date, Collections.emptyList());
            String dayStr = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();

            Map<LocalTime, List<String>> byStartTime = dailySchedules.stream()
                    .collect(Collectors.groupingBy(
                            Schedule::getStartTime,
                            Collectors.mapping(s -> s.getUser().getName(), Collectors.toList())
                    ));

            for (Map.Entry<LocalTime, List<String>> entry : byStartTime.entrySet()) {
                response.add(ScheduleDto.WeeklyResponse.builder()
                        .day(dayStr)
                        .time(entry.getKey().toString())
                        .names(entry.getValue())
                        .build());
            }
        }
        return response;
    }

    // 6. 내 근무 시간표 조회 (알바생용)
    public List<ScheduleDto.MyWeeklyResponse> getMyWeeklySchedule(Long userId, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);

        return scheduleRepository.findMyWeeklySchedule(userId, startDate, endDate).stream()
                .map(s -> ScheduleDto.MyWeeklyResponse.builder()
                        .date(s.getWorkDate())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .build())
                .sorted(Comparator.comparing(ScheduleDto.MyWeeklyResponse::getDate))
                .collect(Collectors.toList());
    }

    // 7. 근무 스케줄 직접 수정 (사장님 - 기존 API용)
    @Transactional
    public Map<String, Object> updateSchedule(Long scheduleId, ScheduleDto.UpdateRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("스케줄 없음"));

        schedule.updateTime(request.getWorkDate(), request.getStartTime(), request.getEndTime());

        Map<String, Object> data = new HashMap<>();
        data.put("scheduleId", schedule.getId());
        data.put("updatedAt", java.time.LocalDateTime.now());
        return data;
    }

    // =========================================================
    // ▼ 정정 요청 승인 시 호출되는 메서드
    // =========================================================
    @Transactional
    public void updateSchedule(Long scheduleId, String afterValue) {
        // 1. 스케줄 조회
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        // 2. 문자열 파싱
        String[] times = afterValue.split("~");
        if (times.length != 2) {
            throw new IllegalArgumentException("시간 형식이 올바르지 않습니다. (예: 09:00~18:00) 입력값: " + afterValue);
        }

        LocalTime newStart = LocalTime.parse(times[0].trim());
        LocalTime newEnd = LocalTime.parse(times[1].trim());

        // 3. 업데이트 수행
        schedule.updateTime(schedule.getWorkDate(), newStart, newEnd);

        log.info("✅ [ScheduleService] 스케줄 정정 완료! ID: {}, 변경시간: {} ~ {}", scheduleId, newStart, newEnd);
    }
}