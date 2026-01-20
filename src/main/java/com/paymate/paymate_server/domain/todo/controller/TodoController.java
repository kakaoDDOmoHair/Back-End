package com.paymate.paymate_server.domain.todo.controller;

import com.paymate.paymate_server.domain.todo.dto.TodoRequest;
import com.paymate.paymate_server.domain.todo.dto.TodoResponse;
import com.paymate.paymate_server.domain.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/todos") // 👈 여기를 깔끔하게 변경!@RequiredArgsConstructor
@RequiredArgsConstructor // 👈 이거 꼭 붙여야 합니다! (Lombok)
public class TodoController {

    private final TodoService todoService;

    // 1. 목록 조회: GET /api/v1/todos?storeId=1
    @GetMapping
    public ResponseEntity<List<TodoResponse>> getTodos(@RequestParam Long storeId) {
        return ResponseEntity.ok(todoService.getTodos(storeId));
    }

    // 2. 등록: POST /api/v1/todos
    @PostMapping
    public ResponseEntity<?> createTodo(@RequestBody TodoRequest request) {
        TodoResponse created = todoService.createTodo(request);
        return ResponseEntity.ok(Map.of("success", true, "data", created));
    }

    // 3. 토글: PATCH /api/v1/todos/{todoId}/toggle
    @PatchMapping("/{todoId}/toggle")
    public ResponseEntity<?> toggleTodo(@PathVariable Long todoId) {
        boolean isDone = todoService.toggleTodo(todoId);
        return ResponseEntity.ok(Map.of("success", true, "isDone", isDone));
    }

    // 4. 삭제: DELETE /api/v1/todos/{todoId}
    @DeleteMapping("/{todoId}")
    public ResponseEntity<?> deleteTodo(@PathVariable Long todoId) {
        todoService.deleteTodo(todoId);
        return ResponseEntity.ok(Map.of("success", true, "message", "삭제되었습니다."));
    }
}