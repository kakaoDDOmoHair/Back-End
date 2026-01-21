package com.paymate.paymate_server.domain.schedule.service;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.schedule.dto.ScheduleDto;
import com.paymate.paymate_server.domain.schedule.entity.Schedule;
import com.paymate.paymate_server.domain.schedule.repository.ScheduleRepository;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;

    // 1. 근무 스케줄 등록
    @Transactional
    public ScheduleDto.CreateResponse createSchedule(ScheduleDto.CreateRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("매장 없음"));
        User worker = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        Schedule schedule = Schedule.builder()
                .store(store)
                .user(worker) // [수정] worker -> user
                .workDate(request.getWorkDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        scheduleRepository.save(schedule);

        return ScheduleDto.CreateResponse.builder()
                .scheduleId(schedule.getId())
                .status("ASSIGNED")
                .build();
    }

    // 2. 월간 스케줄 조회
    public List<ScheduleDto.MonthlyResponse> getMonthlySchedule(Long storeId, int year, int month) {
        // [수정] 리포지토리의 @Query 메서드(findMonthlySchedule) 사용
        return scheduleRepository.findMonthlySchedule(storeId, year, month).stream()
                .map(s -> ScheduleDto.MonthlyResponse.builder()
                        .date(s.getWorkDate().toString())
                        .userId(s.getUser().getId()) // [수정] getWorker() -> getUser()
                        .name(s.getUser().getName()) // [수정] getWorker() -> getUser()
                        .time(s.getStartTime() + "~" + s.getEndTime())
                        .build())
                .collect(Collectors.toList());
    }

    // 5. 주간 근무 시간표 조회 (사장님용)
    public List<ScheduleDto.WeeklyResponse> getWeeklySchedule(Long storeId, LocalDate startDate) {
        // [수정] Store 객체 조회 후 리포지토리에 전달
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("매장 없음"));

        LocalDate endDate = startDate.plusDays(6);

        // [수정] findByStoreAndWorkDateBetween 사용
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
                            Collectors.mapping(s -> s.getUser().getName(), Collectors.toList()) // [수정] getWorker -> getUser
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

        // [수정] 리포지토리의 @Query 메서드(findMyWeeklySchedule) 사용
        return scheduleRepository.findMyWeeklySchedule(userId, startDate, endDate).stream()
                .map(s -> ScheduleDto.MyWeeklyResponse.builder()
                        .date(s.getWorkDate())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .build())
                .sorted(Comparator.comparing(ScheduleDto.MyWeeklyResponse::getDate))
                .collect(Collectors.toList());
    }

    // 7. 근무 스케줄 직접 수정 (사장님)
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
    @Transactional
    public void updateSchedule(Long scheduleId, String newValue) {
        // 1. 스케줄 찾기
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 스케줄이 없습니다. ID=" + scheduleId));

        try {
            // 2. 문자열("09:00") -> LocalTime 변환
            LocalTime time = LocalTime.parse(newValue);

            // 3. 방금 만든 엔티티 메서드 호출! (여기서는 시작 시간을 바꾼다고 가정)
            schedule.updateStartTime(time);

            System.out.println("✅ 스케줄 시작 시간이 수정되었습니다: " + time);

        } catch (Exception e) {
            throw new IllegalArgumentException("시간 형식이 올바르지 않습니다. (예: 09:00) 입력값: " + newValue);
        }
    }
}