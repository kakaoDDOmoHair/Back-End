package com.paymate.paymate_server.domain.schedule.controller;

import com.paymate.paymate_server.domain.schedule.dto.ScheduleDto;
import com.paymate.paymate_server.domain.schedule.service.ScheduleService;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymate.paymate_server.domain.member.entity.User;
import io.swagger.v3.oas.annotations.Operation;


import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final MemberRepository memberRepository;

    // 1. 알바생 근무 스케줄 등록
    @PostMapping
    public ResponseEntity<ScheduleDto.CreateResponse> createSchedule(@RequestBody ScheduleDto.CreateRequest request) {
        return ResponseEntity.ok(scheduleService.createSchedule(request));
    }

    // 2. 월간 스케줄 조회
    @GetMapping("/monthly")
    public ResponseEntity<List<ScheduleDto.MonthlyResponse>> getMonthlySchedule(
            @RequestParam Long storeId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(scheduleService.getMonthlySchedule(storeId, year, month));
    }



    // 5. 주간 근무 시간표 조회 (사장님용). weeks=2 면 이번주+다음주 (알림 센터용)
    @GetMapping("/weekly")
    public ResponseEntity<List<ScheduleDto.WeeklyResponse>> getWeeklySchedule(
            @RequestParam Long storeId,
            @RequestParam LocalDate startDate,
            @RequestParam(required = false, defaultValue = "1") int weeks) {
        return ResponseEntity.ok(scheduleService.getWeeklySchedule(storeId, startDate, weeks));
    }

    // 6. 내 근무 시간표 조회 (알바생용)
    @GetMapping("/my-weekly")
    public ResponseEntity<List<ScheduleDto.MyWeeklyResponse>> getMyWeeklySchedule(
            @RequestParam String username,      // 1. 유저 찾기용
            @RequestParam(required = false) LocalDate startDate // 🌟 2. [추가] 에러 해결을 위해 추가!
    ) {
        User user = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // 만약 프론트에서 startDate를 안 보내면 오늘 날짜로 채워 넣기 (Null 방지)
        if (startDate == null) {
            startDate = LocalDate.now();
        }

        // 🌟 [수정] 인자 2개를 꽉 채워서 보냅니다. (user.getId(), startDate)
        return ResponseEntity.ok(scheduleService.getMyWeeklySchedule(user.getId(), startDate));
    }

    // 7. 근무 스케줄 직접 수정 (사장님)
    @PatchMapping("/{scheduleId}")
    public ResponseEntity<Map<String, Object>> updateSchedule(
            @PathVariable Long scheduleId,
            @RequestBody ScheduleDto.UpdateRequest request) {
        Map<String, Object> data = scheduleService.updateSchedule(scheduleId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", 200);
        response.put("message", "스케줄이 수정되었습니다.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}