package com.paymate.paymate_server.domain.verification.service;

import com.paymate.paymate_server.domain.verification.dto.VerificationDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VerificationService {

    // 계좌 실명 인증 로직
    public VerificationDto.Response verifyAccount(VerificationDto.Request request) {

        // 1. [Mock] 은행 내부 데이터 (실제로는 오픈뱅킹 API로 조회해오는 값)
        String realOwnerName = "고지운";
        String realBankName = "신한은행"; // 혹은 request.getBankName()에 따라 매핑

        // 2. [검증] 사용자가 입력한 예금주명 vs 실제 예금주명 비교
        if (!request.getOwnerName().equals(realOwnerName)) {
            throw new IllegalArgumentException("예금주명이 일치하지 않습니다. (은행 등록명: " + realOwnerName + ")");
        }

        // 3. [성공] 인증 토큰 발급
        String token = "VERIFIED_" + UUID.randomUUID().toString().substring(0, 8);

        return VerificationDto.Response.builder()
                .success(true)
                .message("계좌 실명 인증이 완료되었습니다.")
                .verificationToken(token) // ⭐ 이 토큰을 프론트가 받아서 저장해야 함!
                .bankName(realBankName)
                .ownerName(realOwnerName)
                .build();
    }
}