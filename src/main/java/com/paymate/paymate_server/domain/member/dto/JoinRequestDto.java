package com.paymate.paymate_server.domain.member.dto;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.enums.UserRole;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JoinRequestDto {

    private String username;
    private String email;
    private String password;
    private String name;
    private String phone;

    @Pattern(regexp = "^\\d{6}$", message = "생년월일은 6자리 숫자(예: 980101)로 입력해주세요.")
    private String birthDate;

    // 👇 [수정 1] 역할을 입력받기 위한 필드 추가
    // (Postman에서 "role": "OWNER" 또는 "WORKER" 라고 보내야 함)
    private UserRole role;

    // DTO -> Entity 변환 메서드
    public User toEntity() {
        return User.builder()
                .username(username)
                .email(email)
                .password(password)
                .name(name)
                .phone(phone)
                .birthDate(birthDate)
                .role(role)
                .build();
    }
}