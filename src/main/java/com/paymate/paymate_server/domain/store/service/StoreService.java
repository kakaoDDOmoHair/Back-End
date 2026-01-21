package com.paymate.paymate_server.domain.store.service;

import com.paymate.paymate_server.domain.member.entity.Account; // 👈 import 확인
import com.paymate.paymate_server.domain.member.repository.AccountRepository; // 👈 import 확인
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.enums.UserRole;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.store.dto.CheckBusinessResponse;
import com.paymate.paymate_server.domain.store.dto.DashboardResponse;
import com.paymate.paymate_server.domain.store.dto.JoinRequest;
import com.paymate.paymate_server.domain.store.dto.StoreRequest;
import com.paymate.paymate_server.domain.store.dto.StoreResponse;
import com.paymate.paymate_server.domain.store.entity.Employment;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.EmploymentRepository;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import com.paymate.paymate_server.global.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;
    private final EmploymentRepository employmentRepository;
    private final AccountRepository accountRepository; // 👈 [추가] 계좌 저장을 위해 필요
    private final AesUtil aesUtil;

    // 1. 매장 생성 (계좌 자동 생성 포함)
    public Long createStore(StoreRequest request) {
        // 1-1. 사용자 검증
        User owner = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 1-2. 계좌 인증 토큰 검증
        if (request.getVerificationToken() == null || !request.getVerificationToken().startsWith("VERIFIED_")) {
            throw new IllegalArgumentException("계좌 실명 인증이 완료되지 않았습니다. 인증 후 다시 시도해주세요.");
        }

        // 1-3. 매장 정보 저장
        Store store = Store.builder()
                .owner(owner)
                .name(request.getStoreName())
                .presidentName(request.getOwnerName())
                .businessNumber(request.getBusinessNumber())
                .openingDate(request.getOpeningDate())
                .address(request.getAddress())
                .detailAddress(request.getDetailAddress())
                .taxType(request.getTaxType())
                .category(request.getCategory())
                .storePhone(request.getStorePhone())
                .wifiInfo(request.getWifiInfo())
                .payDay(request.getPayDay())
                .payRule(request.getPayRule())
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .inviteCode(request.getInviteCode())
                .build();

        storeRepository.save(store); // 매장 저장 완료

        // ==========================================================
        // ▼ [추가된 로직] 입력받은 계좌 정보를 Account 테이블에 자동 저장
        // ==========================================================
        try {
            // (1) 계좌번호 암호화 (보안 필수!)
            String encryptedAccountNumber = aesUtil.encrypt(request.getAccountNumber());

            // (2) Account 엔티티 생성
            Account account = Account.builder()
                    .bankName(request.getBankName())       // 요청받은 은행명
                    .accountNumber(encryptedAccountNumber) // 암호화된 계좌번호
                    .balance(0L)                            // 초기 잔액 0원
                    .user(owner)                           // 현재 사장님과 연결
                    .build();

            // (3) DB 저장
            accountRepository.save(account);

        } catch (Exception e) {
            // 암호화 실패 시 예외 처리 (트랜잭션 롤백됨)
            throw new RuntimeException("계좌번호 암호화 및 저장 중 오류가 발생했습니다.", e);
        }
        // ==========================================================

        return store.getId();
    }

    // 2. 매장 상세 조회
    @Transactional(readOnly = true)
    public StoreResponse getStoreDetail(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 매장이 없습니다."));

        return new StoreResponse(store);
    }

    // 3. 사업자 번호 유효성 검사 (Mock)
    public CheckBusinessResponse validateBusinessNumber(String businessNumber) {
        if (businessNumber != null && businessNumber.replace("-", "").length() == 10) {
            return new CheckBusinessResponse(true, "ACTIVE");
        } else {
            return new CheckBusinessResponse(false, "UNKNOWN");
        }
    }

    // 4. 대시보드 통계 조회 (Mock)
    public DashboardResponse getStoreDashboard(Long storeId) {
        return new DashboardResponse(4250000L, 5.2, "2026-01-05");
    }

    // 5. 알바생 매장 가입 (초대코드 입력)
    public Long joinStore(JoinRequest request) {
        User employee = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Store store = storeRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대코드입니다."));

        if (employmentRepository.existsByEmployeeAndStore(employee, store)) {
            throw new IllegalArgumentException("이미 가입된 매장입니다.");
        }

        Employment employment = Employment.builder()
                .employee(employee)
                .store(store)
                .role(UserRole.WORKER)
                .joinedAt(LocalDateTime.now())
                .build();

        employmentRepository.save(employment);

        return store.getId();
    }
}