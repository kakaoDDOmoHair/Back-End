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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    // 5. OCR 스캔 (실제 파일 저장 + 랜덤 결과 반환)
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
        // 안드로이드 에뮬레이터에서 localhost 접근 시 10.0.2.2 사용
        String fileUrl = "http://10.0.2.2:8080/uploads/" + savedFileName;

        // 5. OCR 결과 시뮬레이션 (랜덤 데이터 생성하여 중복 방지)
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("fileUrl", fileUrl); // 저장된 실제 이미지 URL 반환

        Map<String, Object> ocrResult = new HashMap<>();
        int randomNum = (int) (Math.random() * 1000); // 0~999 랜덤 숫자
        ocrResult.put("workerName", "김알바_" + randomNum);
        ocrResult.put("wage", 9860 + (randomNum * 10)); // 시급도 랜덤하게 변동
        ocrResult.put("startDate", "2026-02-01");

        result.put("ocrResult", ocrResult);

        System.out.println("✅ 파일 업로드 완료: " + filePath.toString());
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