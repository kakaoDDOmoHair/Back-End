package com.paymate.paymate_server.domain.verification.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

public class VerificationDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private String bankName;      // 은행명 (예: 국민, 신한)
        private String accountNumber; // 계좌번호
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private boolean success;           // 성공 여부
        private String message;            // 메시지
        private String verificationToken;  // ⭐ 핵심: 검증 완료 토큰
    }
}