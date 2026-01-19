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

    private String email;
    private String name;
    private String role;

    // 📍 이 메서드를 추가해주세요!
    public static MemberResponseDto of(User user) {
        return MemberResponseDto.builder()
                .email(user.getEmail())
                .name(user.getName())
                // Enum인 경우 .name() 또는 .toString()을 붙여야 문자열이 됩니다.
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }
}