package com.paymate.paymate_server.domain.todo.entity;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp; // 👈 추가 필요

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

    // 👇 정렬을 위해 생성 시간은 있는 게 좋습니다! (추가 추천)
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ✅ 핵심: 상태 토글 & 시간 기록 메서드
    public void toggle() {
        this.isCompleted = !this.isCompleted;

        if (this.isCompleted) {
            this.completedAt = LocalDateTime.now(); // 완료하면 현재 시간 기록
        } else {
            this.completedAt = null; // 취소하면 시간 초기화
        }
    }
}