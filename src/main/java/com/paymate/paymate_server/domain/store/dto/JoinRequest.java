package com.paymate.paymate_server.domain.store.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JoinRequest {
    private Long userId;       // 알바생 ID (로그인 구현 전이라 임시로 받음)
    private String inviteCode; // 입력한 초대코드
}