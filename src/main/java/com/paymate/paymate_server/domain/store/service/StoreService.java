package com.paymate.paymate_server.domain.store.service;

import com.paymate.paymate_server.domain.member.entity.Account;
import com.paymate.paymate_server.domain.member.repository.AccountRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;
    private final EmploymentRepository employmentRepository;
    private final AccountRepository accountRepository;
    private final AesUtil aesUtil;

    /**
     * 1. 매장 생성 (사장님 연결 로직 추가)
     */
    public Long createStore(StoreRequest request) {
        // 1-1. 사용자 검증
        User owner = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 1-2. 계좌 인증 토큰 검증
        if (request.getVerificationToken() == null || !request.getVerificationToken().startsWith("VERIFIED_")) {
            throw new IllegalArgumentException("계좌 실명 인증이 완료되지 않았습니다. 인증 후 다시 시도해주세요.");
        }

        // 초대 코드 랜덤 생성 (8자리)
        String uniqueInviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 1-3. 매장 정보 생성
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
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .payDay(request.getPayDay())
                .payRule(request.getPayRule())
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .inviteCode(uniqueInviteCode)
                .build();

        storeRepository.save(store);

        // 🌟 [추가 포인트 1] 사장님 유저 엔티티에 생성된 매장 연결
        // 이 로직이 있어야 DB의 users 테이블 store_id 컬럼에 값이 들어갑니다.
        owner.assignStore(store);

        // 계좌 정보 자동 저장
        try {
            String encryptedAccountNumber = aesUtil.encrypt(request.getAccountNumber());
            Account account = Account.builder()
                    .bankName(request.getBankName())
                    .accountNumber(encryptedAccountNumber)
                    .balance(0L)
                    .user(owner)
                    .build();
            accountRepository.save(account);
        } catch (Exception e) {
            throw new RuntimeException("계좌번호 암호화 중 오류가 발생했습니다.", e);
        }

        return store.getId();
    }

    /**
     * 5. 알바생 매장 가입 (알바생 연결 로직 추가)
     */
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

        // 🌟 [추가 포인트 2] 알바생 유저 엔티티에 가입한 매장 연결
        // 이제 알바생이 /api/v1/users/me 호출 시 storeId를 정상적으로 반환합니다.
        employee.assignStore(store);

        return store.getId();
    }

    @Transactional(readOnly = true)
    public StoreResponse getStoreDetail(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 매장이 없습니다."));
        return new StoreResponse(store);
    }

    public CheckBusinessResponse validateBusinessNumber(String businessNumber) {
        if (businessNumber != null && businessNumber.replace("-", "").length() == 10) {
            return new CheckBusinessResponse(true, "ACTIVE");
        }
        return new CheckBusinessResponse(false, "UNKNOWN");
    }

    public DashboardResponse getStoreDashboard(Long storeId) {
        return new DashboardResponse(4250000L, 5.2, "2026-01-05");
    }
}