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
import jakarta.validation.Valid;

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
    public ResponseEntity<Map<String, Object>> join(@Valid @RequestBody JoinRequestDto dto) {
        // 1. @Valid가 붙어있어서, birthDate가 6자리가 아니면 여기서 바로 에러가 터짐 (자동 방어)

        // 2. DTO를 Entity로 바꿔서 서비스로 넘김
        Long userId = memberService.join(dto.toEntity());

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("message", "회원가입이 완료되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 비밀번호 변경 (PATCH /password)
     * - [변경] 아이디(Username) 기반으로 변경 대상을 찾습니다.
     */
    @PatchMapping("/password")
    public ResponseEntity<PasswordChangeResponseDto> updatePassword(@RequestBody PasswordChangeRequestDto dto) {
        try {
            // 1. 서비스 호출 (서비스 안에서도 findByUsername으로 동작함)
            memberService.changePassword(dto);

            // 2. 성공 응답 반환
            return ResponseEntity.ok(PasswordChangeResponseDto.builder()
                    .success(true)
                    .message("비밀번호가 성공적으로 변경되었습니다.")
                    .username(dto.getUsername()) // [변경] email -> username
                    .build());

        } catch (Exception e) {
            // 3. 실패 응답 반환
            return ResponseEntity.badRequest().body(PasswordChangeResponseDto.builder()
                    .success(false)
                    .message("변경 실패: " + e.getMessage())
                    .username(dto.getUsername()) // [변경] email -> username
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
     */
    @GetMapping("/me")
    public ResponseEntity<MemberResponseDto> getMyInfo(@RequestParam String username) { // 📍 파라미터 변경
        return ResponseEntity.ok(memberService.getMyInfo(username));
    }

    /**
     * 알바생 상세 정보 조회 (GET /{userId}/detail)
     */
    @GetMapping("/detail")
    public ResponseEntity<MemberDetailResponseDto> getMemberDetail(@RequestParam String username) { // 📍 파라미터 변경
        return ResponseEntity.ok(memberService.getMemberDetail(username));
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