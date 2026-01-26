package com.paymate.paymate_server.domain.verification.controller;

import com.paymate.paymate_server.domain.verification.dto.VerificationDto;
import com.paymate.paymate_server.domain.verification.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    // ✅ 1. 실명 인증 API (프론트엔드가 쓰는 것)
    // POST /api/v1/auth/verify-account
    @PostMapping("/verify-account")
    public ResponseEntity<VerificationDto.Response> verifyAccount(@RequestBody VerificationDto.Request request) {
        return ResponseEntity.ok(verificationService.verifyAccount(request));
    }

    // 🛠️ 2. [테스트용] 가짜 계좌 데이터 등록 API (Postman용)
    // POST /api/v1/auth/test/register
    @PostMapping("/test/register")
    public ResponseEntity<String> registerTestAccount(@RequestBody VerificationDto.Request request) {
        Long id = verificationService.createTestAccount(request);
        return ResponseEntity.ok("✅ 계좌 등록 완료 (ID: " + id + ")");
    }
}