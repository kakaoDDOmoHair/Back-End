package com.paymate.paymate_server.domain.member.dto; // 1. 패키지가 제일 위!

import lombok.Getter;           // 2. 임포트
import lombok.NoArgsConstructor;

@Getter                        // 3. 어노테이션은 클래스 바로 위
@NoArgsConstructor
public class WithdrawRequestDto {
    private String username;
    private String password;
    private boolean isAgreed;
}