package com.paymate.paymate_server.domain.salary.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    WAITING("정산 대기"),    // 근무 중, 아직 정산 요청 전 (기존 PENDING 역할)
    REQUESTED("정산 요청중"), // 알바생이 사장님에게 입금 요청을 보낸 상태
    COMPLETED("정산 완료");  // 사장님이 입금 후 완료 처리를 한 상태

    private final String description;
}