package com.paymate.paymate_server.domain.todo.repository;

import com.paymate.paymate_server.domain.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // 로직: (미완료) OR (완료됐지만 완료시간이 cutoffDate보다 이후인 것)
    @Query("SELECT t FROM Todo t " +
            "WHERE t.store.id = :storeId " +
            "AND (t.isCompleted = false OR t.completedAt >= :cutoffDate) " +
            "ORDER BY t.isCompleted ASC, t.id DESC") // 미완료 먼저, 그 뒤 최신순
    List<Todo> findActiveTodos(
            @Param("storeId") Long storeId,
            @Param("cutoffDate") LocalDateTime cutoffDate
    );
}