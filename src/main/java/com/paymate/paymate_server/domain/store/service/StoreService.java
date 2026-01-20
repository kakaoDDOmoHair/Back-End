package com.paymate.paymate_server.domain.store.service;

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

    // 1. 매장 생성
    public Long createStore(StoreRequest request) {
        User owner = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // ▼ [추가된 부분] 계좌 인증 토큰 검증 로직
        // 토큰이 없거나, "VERIFIED_"로 시작하지 않으면 매장 등록을 거부합니다.
        if (request.getVerificationToken() == null || !request.getVerificationToken().startsWith("VERIFIED_")) {
            throw new IllegalArgumentException("계좌 실명 인증이 완료되지 않았습니다. 인증 후 다시 시도해주세요.");
        }

        Store store = Store.builder()
                .owner(owner)
                // 👇 DTO(JSON) 이름 -> Entity 이름 매핑
                .name(request.getStoreName())           // storeName -> name
                .presidentName(request.getOwnerName())  // ownerName -> presidentName
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

        return storeRepository.save(store).getId();
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
        // 1. 알바생 찾기
        User employee = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. 초대코드로 매장 찾기
        Store store = storeRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대코드입니다."));

        // 3. 이미 가입했는지 확인
        if (employmentRepository.existsByEmployeeAndStore(employee, store)) {
            throw new IllegalArgumentException("이미 가입된 매장입니다.");
        }

        // 4. 고용 관계 생성 (Employment)
        Employment employment = Employment.builder()
                .employee(employee)
                .store(store)
                .role(UserRole.WORKER) // 기본 알바생 권한
                .joinedAt(LocalDateTime.now())
                .build();

        employmentRepository.save(employment);

        return store.getId();
    }
}