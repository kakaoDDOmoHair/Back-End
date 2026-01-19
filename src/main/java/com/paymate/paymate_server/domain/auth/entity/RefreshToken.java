package com.paymate.paymate_server.domain.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 📍 @RedisHash 대신 @Entity를 사용합니다 (DB에 저장)
@Entity
@Getter
@Builder
@NoArgsConstructor // JPA는 기본 생성자가 필수입니다.
@AllArgsConstructor
public class RefreshToken {

    @Id
    private String email; // 이메일을 Key(PK)로 사용

    private String token; // Refresh Token 값

    // 토큰 교체(Reissue) 시 사용
    public void updateToken(String token) {
        this.token = token;
    }
}