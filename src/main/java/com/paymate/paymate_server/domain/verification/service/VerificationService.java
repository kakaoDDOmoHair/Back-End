package com.paymate.paymate_server.domain.verification.service;

import com.paymate.paymate_server.domain.member.entity.Account;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.AccountRepository;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
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
    private final AccountRepository accountRepository; // ✅ 추가
    private final MemberRepository memberRepository;   // ✅ 추가

    /**
     * ✅ 1. 계좌 실명 인증 및 실제 계좌 등록
     */
    @Transactional // 🌟 중요: DB 수정을 위해 readOnly를 해제합니다.
    public VerificationDto.Response verifyAccount(VerificationDto.Request request) {

        // 1. 가짜 은행 DB(verification_account)에서 해당 계좌가 있는지 찾음
        BankAccount bankAccount = bankRepository.findFirstByBankNameAndAccountNumber(request.getBankName(), request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("해당 은행에 존재하지 않는 계좌번호입니다."));

        // 2. 예금주 이름 비교
        if (!bankAccount.getOwnerName().equals(request.getOwnerName())) {
            throw new IllegalArgumentException("예금주명이 일치하지 않습니다. (은행 등록명: " + bankAccount.getOwnerName() + ")");
        }

        // 3. 실제 사용자(사장님) 정보 가져오기
        // request에 포함된 userId를 사용합니다.
        User user = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. (ID: " + request.getUserId() + ")"));

        // 4. 인증 성공 시 실제 Account 테이블에 계좌 생성 및 저장
        Account realAccount = Account.builder()
                .bankName(bankAccount.getBankName())
                .accountNumber(bankAccount.getAccountNumber()) // 필요시 암호화 처리
                .balance(0L)
                .user(user) // 🌟 여기서 userId가 반영됩니다!
                .build();
        Account savedAccount = accountRepository.save(realAccount);
        user.updateAccountInfo(savedAccount);
        memberRepository.save(user);

        // 5. 인증 토큰 발급
        String token = "VERIFIED_" + UUID.randomUUID().toString().substring(0, 8);

        return VerificationDto.Response.builder()
                .success(true)
                .message("계좌 실명 인증 및 실제 계좌 등록 완료")
                .verificationToken(token)
                .bankName(bankAccount.getBankName())
                .ownerName(bankAccount.getOwnerName())
                .userId(user.getId())
                .build();
    }

    /**
     * 🛠️ 2. 테스트용 계좌 등록 (은행 전산망 데이터 생성용)
     */
    @Transactional
    public Long createTestAccount(VerificationDto.Request request) {
        BankAccount account = BankAccount.builder()
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .ownerName(request.getOwnerName())
                .userId(request.getUserId())
                .build();

        return bankRepository.save(account).getId();
    }
}