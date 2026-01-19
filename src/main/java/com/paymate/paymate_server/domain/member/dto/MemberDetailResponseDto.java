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
    private String email;
    private String name;
    private String role;
    // (여기에 급여, 근무시간 등 상세 정보 필드가 더 있을 수 있음)

    // 📍 이 메서드가 없어서 에러가 난 것입니다! 아래 코드를 복사해서 넣어주세요.
    public static MemberDetailResponseDto of(User user) {
        return MemberDetailResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name()) // Enum이라면 .name() 또는 .toString()
                .build();
    }
}