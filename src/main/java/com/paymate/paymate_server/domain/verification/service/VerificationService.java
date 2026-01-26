package com.paymate.paymate_server.domain.verification.service;

import com.paymate.paymate_server.domain.verification.dto.VerificationDto;
import com.paymate.paymate_server.domain.verification.entity.BankAccount;
import com.paymate.paymate_server.domain.verification.repository.BankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerificationService {

    private final BankRepository bankRepository;

    /**
     * ✅ 1. 계좌 실명 인증 (메인 로직)
     */
    public VerificationDto.Response verifyAccount(VerificationDto.Request request) {

        // 1. DB(장부)에서 해당 은행/계좌번호가 있는지 찾음
        BankAccount account = bankRepository.findByBankNameAndAccountNumber(request.getBankName(), request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("해당 은행에 존재하지 않는 계좌번호입니다."));

        // 2. 예금주 이름 비교
        if (!account.getOwnerName().equals(request.getOwnerName())) {
            throw new IllegalArgumentException("예금주명이 일치하지 않습니다. (은행 등록명: " + account.getOwnerName() + ")");
        }

        // 3. 인증 성공 -> 토큰 발급
        String token = "VERIFIED_" + UUID.randomUUID().toString().substring(0, 8);

        return VerificationDto.Response.builder()
                .success(true)
                .message("계좌 실명 인증 완료")
                .verificationToken(token)
                .bankName(account.getBankName())
                .ownerName(account.getOwnerName())
                .build();
    }

    /**
     * 🛠️ 2. 테스트용 계좌 등록 (개발자용)
     * Postman으로 이 메서드를 호출해서 가짜 데이터를 DB에 넣습니다.
     */
    @Transactional
    public Long createTestAccount(VerificationDto.Request request) {
        BankAccount account = BankAccount.builder()
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .ownerName(request.getOwnerName())
                .build();

        return bankRepository.save(account).getId();
    }
}