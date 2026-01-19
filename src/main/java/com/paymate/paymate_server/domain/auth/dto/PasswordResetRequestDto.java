package com.paymate.paymate_server.domain.auth.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordResetRequestDto {
    private String newPassword;
}