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

    // 6. 일별 근무 기록 조회 (Daily)
    @GetMapping("/daily")
    public ResponseEntity<List<AttendanceDto.DailyLog>> getDaily(
            @RequestParam Long storeId, @RequestParam String date) {
        return ResponseEntity.ok(attendanceService.getDailyLog(storeId, date));
    }

    // 7. 근무 기록 직접 등록
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> manualRegister(@RequestBody AttendanceDto.ManualRegisterRequest request) {
        Long id = attendanceService.manualRegister(request);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("attendanceId", id, "status", "PENDING")));
    }

    // 8. 근무 기록 수정 요청
    @PostMapping("/requests")
    public ResponseEntity<Map<String, Object>> requestCorrection(@RequestBody AttendanceDto.CorrectionRequest request) {
        Long reqId = attendanceService.requestCorrection(request);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("requestId", reqId, "status", "PENDING")));
    }

    // 9. 요청 처리 (승인/거절)
    @PatchMapping("/requests/{requestId}")
    public ResponseEntity<Map<String, Object>> processRequest(
            @PathVariable Long requestId, @RequestBody AttendanceDto.RequestProcess body) {

        attendanceService.processRequest(requestId, body.getStatus());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "처리 완료",
                "data", Map.of("requestId", requestId, "finalStatus", body.getStatus())
        ));
    }
}