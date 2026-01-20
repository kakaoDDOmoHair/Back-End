package com.paymate.paymate_server.domain.contract.controller;

import com.paymate.paymate_server.domain.contract.dto.ContractRequest;
import com.paymate.paymate_server.domain.contract.dto.ContractResponse;
import com.paymate.paymate_server.domain.contract.enums.ContractStatus;
import com.paymate.paymate_server.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 3. 계약서 목록 조회 (Query Param 방식: ?storeId=1 또는 ?userId=1)
    @GetMapping
    public ResponseEntity<Page<ContractResponse>> getContractList(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) ContractStatus status,
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(contractService.getContractList(storeId, userId, status, pageable));
    }

    // 4. 계약서 스캔 (OCR Mock)
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanContract() {
        // 실제로는 MultipartFile을 받아야 하지만, Mock 테스트를 위해 생략
        return ResponseEntity.ok(contractService.mockOcrScan());
    }

    // 5. 계약서 다운로드 (Mock)
    @GetMapping("/{contractId}/download")
    public ResponseEntity<String> downloadContract(@PathVariable Long contractId) {
        // 실제로는 PDF 파일 스트림을 반환해야 함
        return ResponseEntity.ok("https://mock-s3-bucket.com/contracts/" + contractId + ".pdf");
    }

    // 6. 계약 정보 수정 (PATCH) - 명세서 반영 완료
    @PatchMapping("/{contractId}")
    public ResponseEntity<String> updateContract(@PathVariable Long contractId, @RequestBody ContractRequest request) {
        contractService.updateContract(contractId, request);
        return ResponseEntity.ok("수정되었습니다.");
    }
}