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

    // 계좌 실명 인증 API
    // 최종 URL: /api/v1/auth/verify-account
    @PostMapping("/verify-account") // 👈 [수정 2] account -> verify-account 로 변경
    public ResponseEntity<VerificationDto.Response> verifyAccount(@RequestBody VerificationDto.Request request) {
        return ResponseEntity.ok(verificationService.verifyAccount(request));
    }
}