package com.paymate.paymate_server.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountVerifyResponseDto {
    private String bankName;
    private String ownerName;
    private String verificationToken; // 인증 완료 토큰 (나중에 회원가입시 쓸 수도 있음)
}