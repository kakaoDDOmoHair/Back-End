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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;

    // application.properties에 설정된 경로 (없으면 프로젝트 루트의 uploads 폴더 사용)
    @Value("${file.upload-dir:./uploads/}")
    private String uploadDir;

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

    // 5. OCR 스캔 (가상 데이터 반환 - 테스트용)
    public Map<String, Object> scanContract(MultipartFile file, Long storeId) throws IOException {
        // 1. 저장할 디렉토리 생성
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 2. 파일명 중복 방지를 위한 UUID 적용
        String originalFilename = file.getOriginalFilename();
        String savedFileName = UUID.randomUUID().toString() + "_" + originalFilename;
        Path filePath = uploadPath.resolve(savedFileName);

        // 3. 파일 저장
        file.transferTo(filePath.toFile());

        // 4. 접근 가능한 URL 생성
        String fileUrl = "http://10.0.2.2:8080/uploads/" + savedFileName;

        // 5. 가상 OCR 결과 생성 (테스트용)
        Map<String, Object> ocrResult = new HashMap<>();
        ocrResult.put("workerName", "김민수");
        ocrResult.put("wage", 9860);
        ocrResult.put("startDate", "2026-02-01");  // 프론트 필드명에 맞춤
        ocrResult.put("endDate", "2026-12-31");
        ocrResult.put("workHours", "09:00-18:00");

        // 매장 정보 조회
        Store store = storeRepository.findById(storeId).orElse(null);
        if (store != null) {
            ocrResult.put("storeName", store.getName());
        } else {
            ocrResult.put("storeName", "페이메이트 카페");
        }

        // 계약 상태는 OCR 직후이므로 DRAFT
        ocrResult.put("status", ContractStatus.DRAFT.name());

        // 6. 결과 반환
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("fileUrl", fileUrl);
        result.put("ocrResult", ocrResult);

        log.info("✅ 계약서 스캔 완료 (가상 데이터): {}", ocrResult);
        return result;
    }

    // 6. 계약서 삭제
    public void deleteContract(Long contractId) {
        contractRepository.deleteById(contractId);
    }

    // 7. 다운로드 링크 반환
    public String getDownloadUrl(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약서가 없습니다."));

        if (contract.getFileUrl() != null && !contract.getFileUrl().isEmpty()) {
            return contract.getFileUrl();
        }
        // 파일이 없을 경우 더미 PDF 반환
        return "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf";
    }
}