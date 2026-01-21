package com.paymate.paymate_server.domain.salary.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

public class SalaryDto {

    // 1. 계좌 정보 응답 (이체 버튼 클릭 시)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class AccountResponse {
        private String bank;
        private String account;
        private String holder;
    }

    // 2. 급여 내역 조회 응답 (알바생용 리스트)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class HistoryResponse {
        private Long id;
        private String month; // "1월"
        private Long amount;
        private String status; // WAITING, REQUESTED, COMPLETED
    }

    // 3. 예상 급여 조회 응답 (알바생 실시간)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class EstimatedResponse {
        private String period; // "2026.01.01~"
        private Long amount;
        private Double totalHours;
    }

    // 4. 월별 급여 현황 응답 (사장님용 리스트)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MonthlyResponse {
        private String name;
        private Long amount;
        private String status;
    }

    // 5. 통합 응답 포맷 (월별 조회용 확장)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MonthlySalaryDetailResponse {
        private Integer year;
        private Integer month;
        private Double totalTime;
        private Long totalWage;
        private Map<String, Double> summary; // 날짜별 시간 요약
        private List<MonthlyResponse> list;
    }
    @Getter // 👈 이게 있어야 getStoreId() 등을 호출할 수 있습니다.
    @NoArgsConstructor
    public static class ExecuteRequest {
        private Long storeId;
        private Long userId;
        private Long accountId;
        private int year;
        private int month;
    }
}