package com.paymate.paymate_server.domain.verification.dto;

import lombok.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

public class VerificationDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String bankName;      // 은행명
        private String accountNumber; // 계좌번호
        private String ownerName;
        private Long userId;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private boolean success;           // 성공 여부
        private String message;            // 메시지

        private String verificationToken;

        private String bankName;           // 인증된 은행명
        private String ownerName;          // 인증된 예금주명
    }
}