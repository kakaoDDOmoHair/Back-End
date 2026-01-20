package com.paymate.paymate_server.domain.todo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class TodoRequest {
    private Long storeId;
    private String content;
    // targetDate는 없으면 "오늘"로 처리할 예정
}