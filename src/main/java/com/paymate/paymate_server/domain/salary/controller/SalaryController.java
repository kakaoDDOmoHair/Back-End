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

    // 1. 계좌 정보 조회
    @GetMapping("/{paymentId}/account")
    public ResponseEntity<SalaryDto.AccountResponse> getAccountInfo(@PathVariable Long paymentId) {
        return ResponseEntity.ok(salaryService.getAccountInfo(paymentId));
    }

    // 2. 이체 완료 확정
    @PatchMapping("/{paymentId}/complete")
    public ResponseEntity<Map<String, String>> completePayment(
            @PathVariable Long paymentId,
            @RequestParam Long accountId) {
        String message = salaryService.completePayment(paymentId, accountId);
        return ResponseEntity.ok(Map.of("status", "COMPLETED", "message", message));
    }

    // 3. 명세서 이메일 발송 (프론트 버튼 전용으로 수정됨)
    @PostMapping("/{paymentId}/payslip/send")
    public ResponseEntity<Map<String, Object>> sendPayslip(@PathVariable Long paymentId) {
        // 비동기(@Async)로 메일 발송 트리거
        salaryService.sendPayslipEmail(paymentId);

        return ResponseEntity.ok(Map.of(
                "sent", true,
                "message", "명세서 발송 요청이 접수되었습니다. 잠시 후 메일함을 확인해주세요.",
                "paymentId", paymentId
        ));
    }

    // 4. 급여 내역 조회
    @GetMapping("/history")
    public ResponseEntity<List<SalaryDto.HistoryResponse>> getSalaryHistory(@RequestParam Long userId) {
        return ResponseEntity.ok(salaryService.getSalaryHistory(userId));
    }

    // 5. 정산 요청하기
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestPayment(@RequestBody Map<String, Long> body) {
        salaryService.requestPayment(body.get("paymentId"));
        return ResponseEntity.ok(Map.of("status", "success", "message", "요청이 전송되었습니다."));
    }

    // 6. 예상 급여 조회 (상세 내역 포함됨)
    @GetMapping("/estimated")
    public ResponseEntity<SalaryDto.EstimatedResponse> getEstimatedSalary(
            @RequestParam Long storeId,
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(salaryService.getEstimatedSalary(storeId, userId, year, month));
    }

    // 7. 급여 목록 조회
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

    // 8-1. 개인별 급여대장 엑셀 다운로드
    @GetMapping("/excel/download/user")
    public void downloadUserSalaryExcel(
            @RequestParam Long storeId,
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month,
            HttpServletResponse response) throws IOException {
        salaryService.generateUserSalaryExcel(storeId, userId, year, month, response);
    }

    // 9. 정산하기
    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> executeNewPayment(
            @RequestBody SalaryDto.ExecuteRequest request) {

        String message = salaryService.executeNewPayment(
                request.getStoreId(),
                request.getUserId(),
                request.getAccountId(),
                request.getYear(),
                request.getMonth()
        );
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", message));
    }

    @GetMapping(value = "/{paymentId}/preview", produces = "text/html; charset=utf-8")
    public ResponseEntity<String> getPayslipPreview(@PathVariable Long paymentId) {
        // Service에서 Thymeleaf로 구운(렌더링한) HTML 문자열을 가져옵니다.
        String htmlContent = salaryService.getPayslipHtmlPreview(paymentId);
        return ResponseEntity.ok(htmlContent);
    }
} // 클래스 끝 중괄호