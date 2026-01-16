package com.paymate.paymate_server.domain.todo.entity;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "todo")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // 담당자 (없으면 null 가능 -> 전체 공유)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_id")
    private User assignedUser;

    @Column(nullable = false)
    private String content;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "is_completed")
    private boolean isCompleted;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}