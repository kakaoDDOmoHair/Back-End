package com.paymate.paymate_server.domain.member.controller;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.dto.*;
import com.paymate.paymate_server.domain.member.service.MemberService;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class MemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    /**
     * 회원가입 (POST /join)
     */
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> join(@RequestBody User user) {
        Long userId = memberService.join(user);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("message", "회원가입이 완료되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 비밀번호 변경 (PATCH /password)
     * - 이메일 기반으로 변경 대상을 찾습니다.
     */
    @PatchMapping("/password")
    public ResponseEntity<PasswordChangeResponseDto> updatePassword(@RequestBody PasswordChangeRequestDto dto) {
        try {
            // 1. 서비스에 DTO(이메일, 구비번, 신비번)를 통째로 넘깁니다.
            // (서비스 내부에서 이메일로 유저를 찾고 검증합니다)
            memberService.changePassword(dto);

            // 2. 성공 응답 반환
            return ResponseEntity.ok(PasswordChangeResponseDto.builder()
                    .success(true)
                    .message("비밀번호가 성공적으로 변경되었습니다.")
                    .email(dto.getEmail()) // 요청받은 이메일을 그대로 반환
                    .build());

        } catch (Exception e) {
            // 3. 실패 응답 반환
            return ResponseEntity.badRequest().body(PasswordChangeResponseDto.builder()
                    .success(false)
                    .message("변경 실패: " + e.getMessage())
                    .email(dto.getEmail())
                    .build());
        }
    }

    /**
     * 회원 탈퇴 (DELETE /withdraw)
     * - 이메일과 비밀번호를 검증 후 탈퇴 처리
     */
    @DeleteMapping("/withdraw")
    public ResponseEntity<Map<String, String>> withdraw(@RequestBody WithdrawRequestDto dto) {
        // 1. 탈퇴도 이메일 기반으로 처리하기 위해 dto만 넘깁니다.
        memberService.withdraw(dto);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "정상적으로 탈퇴되었습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 내 정보 조회 (GET /me)
     * - (임시) 아직 토큰 로직이 없으므로 1번 유저로 고정해둠
     */
    @GetMapping("/me")
    public ResponseEntity<MemberResponseDto> getMyInfo(@RequestParam String email) { // 📍 파라미터 변경
        return ResponseEntity.ok(memberService.getMyInfo(email));
    }

    /**
     * 알바생 상세 정보 조회 (GET /{userId}/detail)
     */
    @GetMapping("/detail")
    public ResponseEntity<MemberDetailResponseDto> getMemberDetail(@RequestParam String email) { // 📍 파라미터 변경
        return ResponseEntity.ok(memberService.getMemberDetail(email));
    }

    // 👇 [수정된 코드] UserDetailsImpl 대신 UserDetails 사용
    @PatchMapping("/fcm-token")
    public ResponseEntity<?> updateFcmToken(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody Map<String, String> body) {
        String token = body.get("token");

        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body("토큰이 비어있습니다.");
        }

        // userDetails.getUsername()은 로그인한 사람의 이메일(ID)을 가져옵니다.
        // 이 이메일로 서비스를 호출합니다.
        memberService.updateFcmToken(userDetails.getUsername(), token);

        return ResponseEntity.ok("FCM 토큰 저장 완료");
    }
}