package com.paymate.paymate_server.domain.auth.controller;

import com.paymate.paymate_server.domain.auth.dto.*;
import com.paymate.paymate_server.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1. 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // 2. 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponseDto> reissue(@RequestBody TokenRequestDto request) {
        return ResponseEntity.ok(authService.reissue(request));
    }

    // 3. 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody TokenRequestDto request) {
        authService.logout(request);
        return ResponseEntity.ok("성공적으로 로그아웃 되었습니다.");
    }

    // =========================================================================
    // ▼ [NEW] 추가된 API (ID 찾기, 비번 검증, 계좌 인증)
    // =========================================================================

    // 4. 비밀번호 검증 (회원탈퇴 전 본인확인용)
    @PostMapping("/password/verify")
    public ResponseEntity<Map<String, Boolean>> verifyPassword(@RequestBody Map<String, String> request) {
        // Request Body: { "email": "...", "password": "..." }
        boolean isValid = authService.verifyPassword(request.get("email"), new PasswordVerifyRequestDto() {
            public String getPassword() { return request.get("password"); }
        });

        Map<String, Boolean> response = new HashMap<>();
        response.put("isValid", isValid);
        return ResponseEntity.ok(response);
    }


    // 6. ID 찾기 (인증번호 발송)
    @PostMapping("/find-id/send-code")
    public ResponseEntity<String> sendCode(@RequestBody Map<String, String> request) {
        authService.sendVerificationCode(request.get("email"), request.get("name"));
        return ResponseEntity.ok("인증번호가 발송되었습니다.");
    }

    // 7. ID 찾기 (인증번호 검증)
    @PostMapping("/find-id/verify")
    public ResponseEntity<Map<String, String>> verifyCode(@RequestBody Map<String, String> request) {
        String maskedId = authService.verifyCodeAndGetId(request.get("email"), request.get("authCode"));

        Map<String, String> response = new HashMap<>();
        response.put("maskedId", maskedId);
        response.put("message", "인증 성공");

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ▼ [FIX] 비밀번호 재설정 (Return Type을 Map<String, Object>로 변경하여 오류 해결)
    // =========================================================================

    // 8. PW 재설정 - 유저 확인 (인증번호 발송)
    @PostMapping("/password/reset/check-user")
    public ResponseEntity<Map<String, Object>> checkUserForReset(@RequestBody PasswordResetCheckRequestDto request) {
        authService.checkUserForReset(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", 200); // Integer 값
        response.put("message", "인증번호가 발송되었습니다.");
        response.put("data", null);

        return ResponseEntity.ok(response);
    }

    // 9. PW 재설정 - 인증코드 검증 (리셋 토큰 발급)
    @PostMapping("/password/reset/verify-code")
    public ResponseEntity<Map<String, Object>> verifyCodeForReset(@RequestBody PasswordResetVerifyRequestDto request) {
        String resetToken = authService.verifyCodeForReset(request);

        Map<String, String> data = new HashMap<>();
        data.put("resetToken", resetToken);

        // 여기서 <String, Object>를 써야 code(200)와 data(Map)를 동시에 담을 수 있습니다.
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", 200);
        response.put("message", "인증되었습니다.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    // 10. PW 재설정 - 진짜 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestHeader("resetToken") String resetToken,
            @RequestBody PasswordResetRequestDto request) {

        authService.resetPassword(resetToken, request.getNewPassword());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", 200);
        response.put("message", "비밀번호가 성공적으로 변경되었습니다.");
        response.put("data", null);

        return ResponseEntity.ok(response);
    }
}