package com.paymate.paymate_server.domain.schedule.controller;

import com.paymate.paymate_server.domain.schedule.dto.ScheduleDto;
import com.paymate.paymate_server.domain.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

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



    // 5. 주간 근무 시간표 조회 (사장님용)
    @GetMapping("/weekly")
    public ResponseEntity<List<ScheduleDto.WeeklyResponse>> getWeeklySchedule(
            @RequestParam Long storeId,
            @RequestParam LocalDate startDate) {
        return ResponseEntity.ok(scheduleService.getWeeklySchedule(storeId, startDate));
    }

    // 6. 내 근무 시간표 조회 (알바생용)
    @GetMapping("/my-weekly")
    public ResponseEntity<List<ScheduleDto.MyWeeklyResponse>> getMyWeeklySchedule(
            // TODO: 실제로는 토큰에서 User ID를 꺼내야 하지만, 임시로 1번 유저 사용 or 파라미터 필요
            // 명세서에 userId 파라미터가 없어서 토큰에서 꺼내는게 맞지만,
            // 테스트를 위해 임시로 Parameter나 하드코딩이 필요할 수 있습니다.
            // 여기서는 SecurityContextHolder를 쓰거나, 편의상 파라미터를 추가하겠습니다.
            @RequestParam(required = false, defaultValue = "1") Long userId,
            @RequestParam LocalDate startDate) {
        return ResponseEntity.ok(scheduleService.getMyWeeklySchedule(userId, startDate));
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