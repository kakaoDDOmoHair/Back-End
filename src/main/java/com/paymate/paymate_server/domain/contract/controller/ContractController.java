package com.paymate.paymate_server.domain.contract.controller;

import com.paymate.paymate_server.domain.contract.dto.ContractRequest;
import com.paymate.paymate_server.domain.contract.dto.ContractResponse;
import com.paymate.paymate_server.domain.contract.enums.ContractStatus;
import com.paymate.paymate_server.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    // 1. 계약서 생성
    @PostMapping
    public ResponseEntity<String> createContract(@RequestBody ContractRequest request) {
        Long contractId = contractService.createContract(request);
        return ResponseEntity.ok("계약서 생성 완료! ID: " + contractId);
    }

    // 2. 계약서 상세 조회
    @GetMapping("/{contractId}")
    public ResponseEntity<ContractResponse> getContract(@PathVariable Long contractId) {
        return ResponseEntity.ok(contractService.getContractDetail(contractId));
    }

    // 2-1. 근로계약서 미리보기 (HTML)
    @GetMapping(value = "/{contractId}/preview", produces = "text/html; charset=utf-8")
    public ResponseEntity<String> getContractPreview(@PathVariable Long contractId) {
        String html = contractService.getContractHtmlPreview(contractId);
        return ResponseEntity.ok(html);
    }

    // 3. 계약서 목록 조회 (Query Param 방식: ?storeId=1 또는 ?userId=1)
    @GetMapping
    public ResponseEntity<Page<ContractResponse>> getContractList(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) ContractStatus status,
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(contractService.getContractList(storeId, userId, status, pageable));
    }

    // 4. 계약서 스캔 (파일 업로드 + 가상 OCR + 계약서 생성)
    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> scanContract(
            @RequestParam("file") MultipartFile file,
            @RequestParam("storeId") Long storeId,
            @RequestParam("userId") Long userId
    ) throws IOException {
        return ResponseEntity.ok(contractService.scanContract(file, storeId, userId));
    }

    // 5. 계약서 다운로드
    @GetMapping("/{contractId}/download")
    public ResponseEntity<String> downloadContract(@PathVariable Long contractId) {
        // 실제 저장된 파일 경로 혹은 예시 경로 반환
        return ResponseEntity.ok(contractService.getDownloadUrl(contractId));
    }

    // 6. 계약 정보 수정 (PATCH) - 명세서 반영 완료
    @PatchMapping("/{contractId}")
    public ResponseEntity<String> updateContract(@PathVariable Long contractId, @RequestBody ContractRequest request) {
        contractService.updateContract(contractId, request);
        return ResponseEntity.ok("수정되었습니다.");
    }
}