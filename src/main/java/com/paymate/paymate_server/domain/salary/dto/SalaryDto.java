package com.paymate.paymate_server.domain.salary.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

public class SalaryDto {

    // 1. 계좌 정보 응답 (기존 유지)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class AccountResponse {
        private String bank;
        private String account;
        private String holder;
    }

    // 2. 급여 내역 조회 응답 (기존 유지)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class HistoryResponse {
        private Long id;
        private String month;
        private Long amount;
        private String status;
    }

    // 3. 예상 급여 조회 응답 (상세 항목 추가)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class EstimatedResponse {
        private String period;      // "2026.01.01 ~ 2026.01.21"
        private Double totalHours;  // 총 근무 시간
        private Long baseSalary;    // 기본급 (총 시간 * 시급)
        private Long weeklyAllowance; // [추가] 주휴수당
        private Long tax;           // [추가] 공제 세금 (3.3%)
        private Long amount;        // 최종 실수령액
    }

    // 4. 월별 급여 현황 응답 (기존 유지)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MonthlyResponse {
        private String name;
        private Long amount;
        private String status;
    }

    // 5. 통합 응답 포맷 (기존 유지)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MonthlySalaryDetailResponse {
        private Integer year;
        private Integer month;
        private Double totalTime;
        private Long totalWage;
        private Map<String, Double> summary;
        private List<MonthlyResponse> list;
    }

    // 6. 정산 실행 요청 (기존 유지)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecuteRequest {
        private Long storeId;
        private Long userId;
        private Long accountId;
        private int year;
        private int month;
    }
}