package com.paymate.paymate_server.domain.contract.service;

import com.paymate.paymate_server.domain.contract.dto.ContractRequest;
import com.paymate.paymate_server.domain.contract.dto.ContractResponse;
import com.paymate.paymate_server.domain.contract.entity.Contract;
import com.paymate.paymate_server.domain.contract.enums.ContractStatus;
import com.paymate.paymate_server.domain.contract.repository.ContractRepository;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;
    // NotificationRepository 제거됨 (알림 안 보낼 거니까 필요 없음)

    // 1. 계약서 생성
    public Long createContract(ContractRequest request) {
        User user = memberRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매장입니다."));

        Contract contract = Contract.builder()
                .user(user)
                .store(store)
                .wage(request.getWage())
                .workStartDate(request.getWorkStartDate())
                .workEndDate(request.getWorkEndDate())
                .workHours(request.getWorkHours())
                .status(ContractStatus.DRAFT) // 기본 상태: 작성 중
                .fileUrl(request.getFileUrl())
                .createdAt(LocalDateTime.now())
                .build();

        return contractRepository.save(contract).getId();
    }

    // 2. 계약서 상세 조회
    @Transactional(readOnly = true)
    public ContractResponse getContractDetail(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("해당 계약서가 없습니다."));
        return new ContractResponse(contract);
    }

    // 3. 계약서 목록 조회
    @Transactional(readOnly = true)
    public Page<ContractResponse> getContractList(Long storeId, Long userId, ContractStatus status, Pageable pageable) {
        Page<Contract> contracts;

        if (storeId != null) {
            // 사장님: 내 매장 계약서 조회
            contracts = contractRepository.findByStoreId(storeId, status, pageable);
        } else if (userId != null) {
            // 알바생: 내 계약서 조회
            contracts = contractRepository.findByUserId(userId, status, pageable);
        } else {
            throw new IllegalArgumentException("storeId 또는 userId가 필요합니다.");
        }

        return contracts.map(ContractResponse::new);
    }

    // 4. 계약서 수정 (PATCH)
    public void updateContract(Long contractId, ContractRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("해당 계약서가 없습니다."));

        if (request.getWage() != null) contract.setWage(request.getWage());
        if (request.getWorkHours() != null) contract.setWorkHours(request.getWorkHours());
        if (request.getWorkStartDate() != null) contract.setWorkStartDate(request.getWorkStartDate());
        if (request.getWorkEndDate() != null) contract.setWorkEndDate(request.getWorkEndDate());
        if (request.getStatus() != null) contract.setStatus(request.getStatus());
    }

    // 5. OCR 스캔 Mock
    public Map<String, Object> mockOcrScan() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("fileUrl", "https://mock-s3-bucket.com/contract_sample.jpg");

        Map<String, Object> ocrResult = new HashMap<>();
        ocrResult.put("workerName", "김알바");
        ocrResult.put("wage", 10030);
        ocrResult.put("startDate", "2026-01-01");

        result.put("ocrResult", ocrResult);
        return result;
    }
}