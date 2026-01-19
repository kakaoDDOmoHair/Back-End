package com.paymate.paymate_server.domain.auth.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordResetCheckRequestDto {
    private String name;
    private String email;
    // loginId는 우리 시스템에서 email과 같으므로 생략해도 됨
}