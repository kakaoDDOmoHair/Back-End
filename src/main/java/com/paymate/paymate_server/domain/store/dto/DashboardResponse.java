package com.paymate.paymate_server.domain.store.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardResponse {
    private Long totalCost;    // 총 인건비
    private Double growthRate; // 전월 대비 등락률
    private String payDate;    // 급여 지급일
}