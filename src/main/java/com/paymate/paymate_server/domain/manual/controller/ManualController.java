package com.paymate.paymate_server.domain.manual.controller;

import com.paymate.paymate_server.domain.manual.dto.ManualRequest;
import com.paymate.paymate_server.domain.manual.dto.ManualResponse;
import com.paymate.paymate_server.domain.manual.service.ManualService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/manuals")
@RequiredArgsConstructor
public class ManualController {

    private final ManualService manualService;

    // 1. 목록 조회
    @GetMapping
    public ResponseEntity<?> getManuals(@RequestParam Long storeId) {
        List<ManualResponse> manuals = manualService.getManuals(storeId);
        return ResponseEntity.ok(Map.of("status", "success", "data", manuals));
    }

    // 2. 상세 조회
    @GetMapping("/{manualId}")
    public ResponseEntity<?> getManualDetail(@PathVariable Long manualId) {
        ManualResponse manual = manualService.getManualDetail(manualId);
        return ResponseEntity.ok(manual);
    }

    // 3. 등록
    @PostMapping
    public ResponseEntity<?> createManual(@RequestBody ManualRequest request) {
        Long manualId = manualService.createManual(request);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("manualId", manualId)));
    }

    // 4. 수정
    @PatchMapping("/{manualId}")
    public ResponseEntity<?> updateManual(@PathVariable Long manualId, @RequestBody ManualRequest request) {
        manualService.updateManual(manualId, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "수정되었습니다."));
    }

    // 5. 삭제
    @DeleteMapping("/{manualId}")
    public ResponseEntity<?> deleteManual(@PathVariable Long manualId) {
        manualService.deleteManual(manualId);
        return ResponseEntity.ok(Map.of("success", true, "message", "삭제되었습니다."));
    }
}