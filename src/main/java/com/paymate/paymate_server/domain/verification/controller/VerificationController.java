package com.paymate.paymate_server.domain.verification.controller;

import com.paymate.paymate_server.domain.verification.dto.VerificationDto;
import com.paymate.paymate_server.domain.verification.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    // 계좌 실명 확인 API
    @PostMapping("/account")
    public ResponseEntity<VerificationDto.Response> verifyAccount(@RequestBody VerificationDto.Request request) {
        return ResponseEntity.ok(verificationService.verifyAccount(request));
    }
}