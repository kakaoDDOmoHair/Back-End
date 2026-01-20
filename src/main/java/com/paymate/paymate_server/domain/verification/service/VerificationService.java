package com.paymate.paymate_server.domain.verification.service;

import com.paymate.paymate_server.domain.verification.dto.VerificationDto;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class VerificationService {

    public VerificationDto.Response verifyAccount(VerificationDto.Request request) {
        // [Mock 로직] 계좌번호가 10자리 미만이면 "없는 계좌"라고 가정
        if (request.getAccountNumber() == null || request.getAccountNumber().length() < 10) {
            return VerificationDto.Response.builder()
                    .success(false)
                    .message("올바르지 않은 계좌번호입니다.")
                    .verificationToken(null)
                    .build();
        }

        // [성공 시] "VERIFIED_"로 시작하는 랜덤 토큰 발급
        String token = "VERIFIED_" + UUID.randomUUID().toString();

        return VerificationDto.Response.builder()
                .success(true)
                .message("계좌 실명 인증이 완료되었습니다.")
                .verificationToken(token) // 이 토큰을 프론트엔드가 받아서 가입 때 씀
                .build();
    }
}