package com.paymate.paymate_server.domain.salary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        private Double totalHours; // 월 누적 근로시간
        private String status;
    }

    // 알바생용 현재 월 급여 조회 응답
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CurrentMonthSalaryResponse {
        private Long paymentId;
        private Integer year;
        private Integer month;
        private Long amount;           // 최종 지급액
        private String status;         // COMPLETED, WAITING, REQUESTED 등
        private Long baseSalary;       // 기본급
        private Long weeklyAllowance;  // 주휴수당
        private Long tax;              // 세금
        private Double totalHours;     // 총 근무 시간
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
        private Long paymentId;
        /** status가 REQUESTED일 때만 설정. 정산 요청 시각 (KST ISO-8601, 예: "2026-01-22T11:30:00+09:00"). 프론트는 requestedAt 또는 requested_at 읽음. */
        private String requestedAt;

        /** JSON에 requested_at 키로도 내려줌 (프론트 필드명 호환) */
        @JsonProperty("requested_at")
        public String getRequested_at() {
            return requestedAt;
        }
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

    // 정산 요청 응답 (일한 시간, 요청 금액 포함)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class RequestResponse {
        private Long paymentId;
        private Integer year;
        private Integer month;
        private Long amount;           // 요청 금액
        private Double totalHours;     // 일한 시간
        private String status;         // REQUESTED
        private Long baseSalary;       // 기본급
        private Long weeklyAllowance;  // 주휴수당
        private Long tax;              // 세금
    }
}