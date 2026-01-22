package com.paymate.paymate_server.domain.member.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PasswordChangeRequestDto {
    private String username;           // 📍 추가: 누구의 비번을 바꿀지 식별
    private String currentPassword;
    private String newPassword;
}