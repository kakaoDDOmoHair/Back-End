package com.paymate.paymate_server.domain.salary.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

public class SalaryDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AccountResponse {
        private String bank;
        private String account;
        private String holder;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class HistoryResponse {
        private Long id;
        private String month;
        private Long amount;
        private String status;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class EstimatedResponse {
        private String period;
        private Long amount;
        private Double totalHours;

        // ▼▼▼ [추가] 상세 명세서를 위한 필드들 ▼▼▼
        private Long baseSalary;      // 기본급
        private Long weeklyAllowance; // 주휴수당
        private Long tax;             // 세금 (3.3%)
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MonthlyResponse {
        private String name;
        private Long amount;
        private String status;
        private Long accountId;
        private Long userId;
    }

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

    @Getter
    @NoArgsConstructor
    public static class ExecuteRequest {
        private Long storeId;
        private Long userId;
        private Long accountId;
        private int year;
        private int month;
    }
}