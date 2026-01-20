package com.paymate.paymate_server.domain.todo.dto;

import com.paymate.paymate_server.domain.todo.entity.Todo;
import lombok.Getter;

@Getter
public class TodoResponse {
    private Long todoId;
    private String content;
    private boolean isDone; // API 명세에 맞춤 (isCompleted -> isDone)

    public TodoResponse(Todo todo) {
        this.todoId = todo.getId();
        this.content = todo.getContent();
        this.isDone = todo.isCompleted(); // 여기서 매핑!
    }
}