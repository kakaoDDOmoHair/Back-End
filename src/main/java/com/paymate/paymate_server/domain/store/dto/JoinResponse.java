package com.paymate.paymate_server.domain.store.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor // 👈 모든 필드를 받는 생성자 자동 생성 ("성공", storeId) 가능해짐!
public class JoinResponse {
    private String message;
    private Long storeId;
}