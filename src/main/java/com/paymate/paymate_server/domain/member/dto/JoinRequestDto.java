package com.paymate.paymate_server.domain.member.dto;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.enums.UserRole;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JoinRequestDto {

    private String username; // [추가] 로그인 아이디
    private String email;    // 연락처용 이메일
    private String password;
    private String name;
    private String phone;

    // [유지] 숫자 6자리 제한 (예: 980101)
    @Pattern(regexp = "^\\d{6}$", message = "생년월일은 6자리 숫자(예: 980101)로 입력해주세요.")
    private String birthDate;

    // DTO -> Entity 변환 메서드
    public User toEntity() {
        return User.builder()
                .username(username) // [추가] 엔티티에 아이디 전달
                .email(email)
                .password(password)
                .name(name)
                .phone(phone)
                .birthDate(birthDate)
                .role(UserRole.WORKER) // 기본값 설정 (알바생)
                .build();
    }
}