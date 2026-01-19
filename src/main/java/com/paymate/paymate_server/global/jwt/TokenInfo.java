package com.paymate.paymate_server.global.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class TokenInfo {
    private String grantType;   // Bearer (토큰 타입)
    private String accessToken; // 찐 입장권
    private String refreshToken;// 재발급용 예비표
}