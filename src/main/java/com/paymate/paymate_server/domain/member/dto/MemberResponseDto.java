package com.paymate.paymate_server.domain.member.dto;

import com.paymate.paymate_server.domain.member.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponseDto {

    private String username; // [추가] 아이디
    private String email;    // 이메일 (연락처용)
    private String name;
    private String role;

    // 📍 User 엔티티를 DTO로 변환하는 메서드
    public static MemberResponseDto of(User user) {
        return MemberResponseDto.builder()
                .username(user.getUsername()) // [추가] 엔티티의 username을 넣음
                .email(user.getEmail())
                .name(user.getName())
                // Enum인 경우 .name()을 붙여야 문자열("OWNER", "WORKER")이 됩니다.
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }
}