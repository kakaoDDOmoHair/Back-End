package com.paymate.paymate_server.domain.attendance.controller;

import com.paymate.paymate_server.domain.attendance.dto.AttendanceDto;
import com.paymate.paymate_server.domain.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // 1. 출근 하기
    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceDto.ClockInResponse> clockIn(@RequestBody AttendanceDto.ClockInRequest request) {
        return ResponseEntity.ok(attendanceService.clockIn(request));
    }

    // 2. 퇴근 하기
    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceDto.ClockOutResponse> clockOut(@RequestBody AttendanceDto.ClockOutRequest request) {
        return ResponseEntity.ok(attendanceService.clockOut(request));
    }

    // 3. 월간 근무 기록 조회
    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthly(
            @RequestParam Long userId, // 테스트용 파라미터
            @RequestParam int year,
            @RequestParam int month) {

        List<AttendanceDto.AttendanceLog> list = attendanceService.getMonthlyLog(userId, year, month);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", 200);
        response.put("data", list);

        return ResponseEntity.ok(response);
    }

    // 4. 근무 기록 직접 수정 (사장님)
    @PatchMapping("/{attendanceId}/modify")
    public ResponseEntity<Map<String, Object>> modify(
            @PathVariable Long attendanceId,
            @RequestBody AttendanceDto.ModifyRequest request) {

        attendanceService.modifyAttendance(attendanceId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "근무 기록이 수정되었습니다.");

        return ResponseEntity.ok(response);
    }
    // 5. 실시간 근무 현황 조회 (Today)
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getToday(@RequestParam Long storeId) {
        AttendanceDto.TodayResponse data = attendanceService.getTodayStatus(storeId);
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    // 6. 일별 근무 기록 조회 (Daily) — 응답을 { success, code, data } 형태로 통일
    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> getDaily(
            @RequestParam Long storeId, @RequestParam String date) {
        List<AttendanceDto.DailyLog> list = attendanceService.getDailyLog(storeId, date);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", 200);
        response.put("data", list);
        return ResponseEntity.ok(response);
    }

    // 6-1. 현재 출근 중인 기록 조회 (퇴근 전 attendanceId 복구용)
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentOpen(@RequestParam Long userId) {
        return ResponseEntity.ok(attendanceService.getCurrentOpenAttendance(userId));
    }

    // 7. 근무 기록 직접 등록
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> manualRegister(@RequestBody AttendanceDto.ManualRegisterRequest request) {
        Long id = attendanceService.manualRegister(request);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("attendanceId", id, "status", "PENDING")));
    }


}