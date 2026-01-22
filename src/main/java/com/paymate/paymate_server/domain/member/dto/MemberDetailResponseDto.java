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
public class MemberDetailResponseDto {

    private Long id;
    private String username; // [추가] 아이디
    private String email;
    private String name;
    private String role;
    // (여기에 급여, 근무시간 등 상세 정보 필드가 더 있을 수 있음)

    public static MemberDetailResponseDto of(User user) {
        return MemberDetailResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername()) // [추가] 엔티티에서 아이디 가져오기
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }
}