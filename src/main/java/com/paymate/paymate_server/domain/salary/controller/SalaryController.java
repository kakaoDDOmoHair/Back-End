package com.paymate.paymate_server.domain.salary.controller;

import com.paymate.paymate_server.domain.salary.dto.SalaryDto;
import com.paymate.paymate_server.domain.salary.service.SalaryService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/salary")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    // 1. 계좌 정보 조회 (사장님이 이체 버튼 클릭 시 복호화하여 반환)
    @GetMapping("/{paymentId}/account")
    public ResponseEntity<SalaryDto.AccountResponse> getAccountInfo(@PathVariable Long paymentId) {
        return ResponseEntity.ok(salaryService.getAccountInfo(paymentId));
    }

    // 2. 이체 완료 확정 (사장님이 입금 후 '완료' 처리)
    @PatchMapping("/{paymentId}/complete")
    public ResponseEntity<Map<String, String>> completePayment(
            @PathVariable Long paymentId,
            @RequestParam Long accountId) { // 👈 입금받을 계좌 ID를 추가로 받음
        String message = salaryService.completePayment(paymentId, accountId);
        return ResponseEntity.ok(Map.of("status", "COMPLETED", "message", message));
    }

    // 3. 명세서 이메일 발송 (PDF 생성 및 전송 트리거)
    @PostMapping("/{paymentId}/payslip/send")
    public ResponseEntity<Map<String, Boolean>> sendPayslip(@PathVariable Long paymentId) {
        salaryService.sendPayslipEmail(paymentId); // @Async로 비동기 처리 권장
        return ResponseEntity.ok(Map.of("sent", true));
    }

    // 4. 급여 내역 조회 (알바생용 월별 히스토리)
    @GetMapping("/history")
    public ResponseEntity<List<SalaryDto.HistoryResponse>> getSalaryHistory(@RequestParam Long userId) {
        // 실제 운영 시에는 @AuthenticationPrincipal 등으로 현재 유저 ID를 가져옵니다.
        return ResponseEntity.ok(salaryService.getSalaryHistory(userId));
    }

    // 5. 정산 요청하기 (알바생이 사장님에게 요청)
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestPayment(@RequestBody Map<String, Long> body) {
        salaryService.requestPayment(body.get("paymentId"));
        return ResponseEntity.ok(Map.of("status", "success", "message", "요청이 전송되었습니다."));
    }

    // 6. 예상 급여 조회 (이번 달 실시간 예상치)
    @GetMapping("/estimated")
    public ResponseEntity<SalaryDto.EstimatedResponse> getEstimatedSalary(
            @RequestParam Long storeId,
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(salaryService.getEstimatedSalary(storeId, userId, year, month));
    }

    // 7. 급여 목록 조회 (사장님용 해당 월 전체 현황)
    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlySalaries(
            @RequestParam Long storeId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(salaryService.getMonthlySalaryList(storeId, year, month));
    }

    // 8. 급여대장 엑셀 다운로드
    @GetMapping("/excel/download")
    public void downloadSalaryExcel(
            @RequestParam Long storeId,
            @RequestParam int year,
            @RequestParam int month,
            HttpServletResponse response) throws IOException {
        salaryService.generateSalaryExcel(storeId, year, month, response);
    }

    // 9. 정산하기
    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> executeNewPayment(
            @RequestBody SalaryDto.ExecuteRequest request) { // 👈 @RequestParam 대신 @RequestBody 사용

        String message = salaryService.executeNewPayment(
                request.getStoreId(),
                request.getUserId(),
                request.getAccountId(),
                request.getYear(),
                request.getMonth()
        );
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", message));
    }
}