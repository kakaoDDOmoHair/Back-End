package com.paymate.paymate_server.domain.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCode {

    @Id
    private String email;      // 이메일 (Key)
    private String code;       // 인증번호
    private LocalDateTime expiryDate; // 만료 시간

    // 만료 여부 확인
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}