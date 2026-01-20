package com.paymate.paymate_server.domain.schedule.service;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.schedule.dto.ScheduleDto;
import com.paymate.paymate_server.domain.schedule.entity.Schedule;
import com.paymate.paymate_server.domain.schedule.entity.ScheduleRequest;
import com.paymate.paymate_server.domain.schedule.enums.ScheduleRequestStatus;
import com.paymate.paymate_server.domain.schedule.repository.ScheduleRepository;
import com.paymate.paymate_server.domain.schedule.repository.ScheduleRequestRepository;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleRequestRepository scheduleRequestRepository;
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

    // 3. 근무 스케줄 수정 요청 (알바생 -> 사장님)
    @Transactional
    public Map<String, Object> requestModification(ScheduleDto.ModificationRequest requestDto) {
        Schedule schedule = scheduleRepository.findById(requestDto.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("스케줄 없음"));

        ScheduleRequest request = ScheduleRequest.builder()
                .schedule(schedule)
                .requestType(requestDto.getRequestType())
                .beforeTime(requestDto.getBeforeTime())
                .afterTime(requestDto.getAfterTime())
                .reason(requestDto.getReason())
                .status(ScheduleRequestStatus.PENDING)
                .build();

        scheduleRequestRepository.save(request);

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", request.getId());
        data.put("status", "PENDING");
        return data;
    }

    // 4. 근무 스케줄 수정 요청 처리 (사장님 승인/거절)
    @Transactional
    public Map<String, Object> handleModificationRequest(Long requestId, ScheduleDto.HandleRequest dto) {
        ScheduleRequest request = scheduleRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("요청 내역 없음"));

        request.updateStatus(dto.getStatus());

        if (dto.getStatus() == ScheduleRequestStatus.APPROVED) {
            Schedule schedule = request.getSchedule();
            String[] times = request.getAfterTime().split("~");
            LocalTime newStart = LocalTime.parse(times[0].trim());
            LocalTime newEnd = LocalTime.parse(times[1].trim());

            schedule.updateTime(schedule.getWorkDate(), newStart, newEnd);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", request.getId());
        data.put("status", dto.getStatus().toString());
        data.put("processedAt", request.getProcessedAt());
        return data;
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
}