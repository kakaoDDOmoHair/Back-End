package com.paymate.paymate_server.domain.member.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PasswordChangeResponseDto {
    private boolean success;
    private String message;
    private String email;
}