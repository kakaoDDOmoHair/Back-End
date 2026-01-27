package com.paymate.paymate_server.domain.auth.dto; // 패키지 경로는 상황에 맞게 조정

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private String accessToken; // 토큰
    private String refreshToken;
    private Long userId;        // 🌟 [핵심] 유저 고유 번호 (PK)
    private String role;        // 역할 (OWNER, WORKER)
    private String name;        // 사용자 이름
}