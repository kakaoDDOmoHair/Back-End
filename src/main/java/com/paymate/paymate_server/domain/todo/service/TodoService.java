package com.paymate.paymate_server.domain.todo.service;

import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import com.paymate.paymate_server.domain.todo.dto.TodoRequest;
import com.paymate.paymate_server.domain.todo.dto.TodoResponse;
import com.paymate.paymate_server.domain.todo.entity.Todo;
import com.paymate.paymate_server.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TodoService {

    private final TodoRepository todoRepository;
    private final StoreRepository storeRepository;

    // 1. 등록
    public TodoResponse createTodo(TodoRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("매장을 찾을 수 없습니다."));

        Todo todo = Todo.builder()
                .store(store)
                .content(request.getContent())
                .targetDate(LocalDate.now()) // 기본값: 오늘 날짜
                .isCompleted(false)
                .build();

        return new TodoResponse(todoRepository.save(todo));
    }

    // 2. 조회 (24시간 로직)
    @Transactional(readOnly = true)
    public List<TodoResponse> getTodos(Long storeId) {
        // 기준: 현재 시간 - 24시간
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(24);

        return todoRepository.findActiveTodos(storeId, cutoffDate).stream()
                .map(TodoResponse::new)
                .collect(Collectors.toList());
    }

    // 3. 토글
    public boolean toggleTodo(Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("할 일을 찾을 수 없습니다."));

        todo.toggle(); // 엔티티 메서드 호출 (상태 변경 + 완료시간 기록)

        return todo.isCompleted();
    }

    // 4. 삭제
    public void deleteTodo(Long todoId) {
        todoRepository.deleteById(todoId);
    }
}