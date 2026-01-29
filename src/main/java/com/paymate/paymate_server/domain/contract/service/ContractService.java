package com.paymate.paymate_server.domain.contract.service;

import com.lowagie.text.pdf.BaseFont;
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
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;
    private final TemplateEngine templateEngine;

    // application.properties에 설정된 경로 (기본: 컨테이너 내 /app/uploads)
    @Value("${file.upload-dir:/app/uploads}")
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

    // 2-1. 근로계약서 미리보기 HTML 생성 (템플릿 렌더링)
    @Transactional(readOnly = true)
    public String getContractHtmlPreview(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("해당 계약서가 없습니다."));

        Context context = new Context();
        context.setVariable("contractId", contract.getId());
        context.setVariable("workerName", contract.getUser().getName());
        context.setVariable("storeName", contract.getStore().getName());
        context.setVariable("wage", contract.getWage());
        context.setVariable("startDate", contract.getWorkStartDate());
        context.setVariable("endDate",
                contract.getWorkEndDate() != null ? contract.getWorkEndDate() : "기간 없음");
        context.setVariable("issuedDate", LocalDate.now());

        String statusLabel = "계약 중";
        if (contract.getStatus() == ContractStatus.ENDED) {
            statusLabel = "계약 종료";
        } else if (contract.getStatus() == ContractStatus.DRAFT) {
            statusLabel = "작성 중";
        }
        context.setVariable("statusLabel", statusLabel);

        // templates/contract-template.html 기준
        return templateEngine.process("contract-template", context);
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

    // 5. OCR 스캔 (가상 데이터 + 계약서 생성 - 테스트용)
    public Map<String, Object> scanContract(MultipartFile file, Long storeId, Long userId) throws IOException {
        // 1. 저장할 디렉토리 생성 (절대 경로 기준)
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath); // 이미 있어도 예외 안 남

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

        // 실제 근로자 이름 사용
        User worker = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        ocrResult.put("workerName", worker.getName());

        // 시급은 데모용으로 고정값 사용 (추후 확장 가능)
        ocrResult.put("wage", 9860);
        ocrResult.put("startDate", "2026-02-01");  // 프론트 필드명에 맞춤
        ocrResult.put("endDate", "2026-12-31");

        // 매장 정보 조회
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매장입니다."));
        ocrResult.put("storeName", store.getName());

        // 계약 상태는 OCR 직후이므로 DRAFT
        ocrResult.put("status", ContractStatus.DRAFT.name());

        // 6. OCR 결과를 기반으로 실제 Contract 엔티티 생성 (가짜 데이터로 계약서 한 건 저장)

        // startDate / endDate 파싱 (scanContract 내에서 우리가 넣은 값이므로 포맷 보장)
        LocalDate startDate = LocalDate.parse((String) ocrResult.get("startDate"));
        LocalDate endDate = LocalDate.parse((String) ocrResult.get("endDate"));
        Integer wage = (Integer) ocrResult.get("wage");

        Contract contract = Contract.builder()
                .user(worker)
                .store(store)
                .wage(wage)
                .workStartDate(startDate)
                .workEndDate(endDate)
                // 근로 시간(출퇴근 시간)은 계약서에 저장하지 않음
                .status(ContractStatus.DRAFT)
                .fileUrl(fileUrl)
                .createdAt(LocalDateTime.now())
                .build();

        Contract saved = contractRepository.save(contract);

        // 7. 결과 반환
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("fileUrl", fileUrl);
        result.put("ocrResult", ocrResult);
        result.put("contractId", saved.getId());

        log.info("✅ 계약서 스캔 완료 (가상 데이터): {}", ocrResult);
        return result;
    }

    // 6. 계약서 삭제
    public void deleteContract(Long contractId) {
        contractRepository.deleteById(contractId);
    }

    // 7. 다운로드 링크 반환 (근로계약서 템플릿 기반 PDF 생성)
    public String getDownloadUrl(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약서가 없습니다."));

        try {
            // 1) 템플릿으로 HTML 생성
            String html = getContractHtmlPreview(contractId);

            // 2) 저장 경로 생성
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String pdfFileName = "contract-" + contract.getId() + ".pdf";
            Path pdfPath = uploadPath.resolve(pdfFileName);

            // 3) HTML -> PDF 렌더링
            try (FileOutputStream fos = new FileOutputStream(pdfPath.toFile())) {
                ITextRenderer renderer = new ITextRenderer();

                // 한글 폰트 설정 (Windows 기준, 없으면 기본 폰트 사용)
                String fontPath = "C:/Windows/Fonts/malgun.ttf";
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    renderer.getFontResolver().addFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }

                renderer.setDocumentFromString(html);
                renderer.layout();
                renderer.createPDF(fos);
            }

            // 4) 앱에서 접근 가능한 URL 반환 (기존 업로드 URL 패턴 재사용)
            return "http://10.0.2.2:8080/uploads/" + pdfFileName;
        } catch (Exception e) {
            log.error("계약서 PDF 생성 실패 - contractId: {}", contractId, e);
            throw new IllegalStateException("계약서 파일 생성에 실패했습니다.");
        }
    }
}