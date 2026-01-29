package com.paymate.paymate_server.domain.schedule.service;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.notification.enums.NotificationType;
import com.paymate.paymate_server.domain.notification.service.NotificationService;
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
    private final NotificationService notificationService;

    // 1. 근무 스케줄 등록
    @Transactional
    public ScheduleDto.CreateResponse createSchedule(ScheduleDto.CreateRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("매장 없음"));
        User worker = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // DTO(String) -> Entity(LocalTime/Integer) 변환
        LocalTime start = LocalTime.parse(request.getStartTime());
        LocalTime end = LocalTime.parse(request.getEndTime());
        Integer breakMin = request.getBreakTime() != null ? Integer.parseInt(request.getBreakTime()) : 0;

        Schedule schedule = Schedule.builder()
                .store(store)
                .user(worker)
                .workDate(request.getWorkDate())
                .startTime(start)
                .endTime(end)
                .breakTime(breakMin)
                .build();

        scheduleRepository.save(schedule);

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
                        .time(s.getStartTime().toString().substring(0, 5) + "~" + s.getEndTime().toString().substring(0, 5))
                        .build())
                .collect(Collectors.toList());
    }

    // 3. 주간 근무 시간표 조회 (사장님용)
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

            Map<String, List<ScheduleDto.WeeklyResponse.WorkerInfo>> byTimeRange = dailySchedules.stream()
                    .collect(Collectors.groupingBy(
                            s -> s.getStartTime().toString().substring(0, 5) + "~" + s.getEndTime().toString().substring(0, 5),
                            Collectors.mapping(s -> ScheduleDto.WeeklyResponse.WorkerInfo.builder()
                                    .scheduleId(s.getId())
                                    .name(s.getUser().getName())
                                    .breakTime(s.getBreakTime()) // Integer로 반환
                                    .build(), Collectors.toList())
                    ));

            for (Map.Entry<String, List<ScheduleDto.WeeklyResponse.WorkerInfo>> entry : byTimeRange.entrySet()) {
                response.add(ScheduleDto.WeeklyResponse.builder()
                        .day(dayStr)
                        .time(entry.getKey())
                        .workers(entry.getValue())
                        .build());
            }
        }
        return response;
    }

    // 4. 내 근무 시간표 조회 (알바생용)
    public List<ScheduleDto.MyWeeklyResponse> getMyWeeklySchedule(Long userId, LocalDate startDate) {
        return scheduleRepository.findAllByUser_IdOrderByWorkDateDesc(userId).stream()
                .map(s -> ScheduleDto.MyWeeklyResponse.builder()
                        .date(s.getWorkDate())
                        .startTime(s.getStartTime().toString().substring(0, 5))
                        .endTime(s.getEndTime().toString().substring(0, 5))
                        .breakTime(s.getBreakTime()) // Integer 반환 (DTO가 Integer일 때)
                        .build())
                .collect(Collectors.toList());
    }

    // 5. 사장님 직접 수정 (수정 요청 처리)
    @Transactional
    public Map<String, Object> updateSchedule(Long scheduleId, ScheduleDto.UpdateRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("스케줄 없음"));

        // DTO(String) -> LocalTime 변환
        LocalTime start = LocalTime.parse(request.getStartTime());
        LocalTime end = LocalTime.parse(request.getEndTime());

        // breakTime 변환
        Integer breakMin = (request.getBreakTime() != null)
                ? Integer.parseInt(request.getBreakTime())
                : 0;

        // 엔티티 업데이트 (인자 4개 전달)
        schedule.updateTime(request.getWorkDate(), start, end, breakMin);

        Map<String, Object> data = new HashMap<>();
        data.put("scheduleId", schedule.getId());
        data.put("updatedAt", java.time.LocalDateTime.now());
        return data;
    }

    // 6. 알바생 정정 요청 승인 시 호출
    @Transactional
    public void updateSchedule(Long scheduleId, String afterValue) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        String[] times = afterValue.split("~");
        if (times.length != 2) {
            throw new IllegalArgumentException("시간 형식이 올바르지 않습니다.");
        }

        LocalTime newStart = LocalTime.parse(times[0].trim());
        LocalTime newEnd = LocalTime.parse(times[1].trim());

        // 기존 휴게시간 유지하여 4개 인자 전달
        schedule.updateTime(schedule.getWorkDate(), newStart, newEnd, schedule.getBreakTime());

        log.info("✅ [ScheduleService] 스케줄 정정 완료! ID: {}, 변경시간: {} ~ {}", scheduleId, newStart, newEnd);
    }
}