package com.paymate.paymate_server.domain.auth.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordResetCheckRequestDto {
    private String username;
    private String name;
    private String email;
}