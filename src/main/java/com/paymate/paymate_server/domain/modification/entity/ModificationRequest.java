package com.paymate.paymate_server.domain.modification.entity;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.modification.enums.RequestStatus;
import com.paymate.paymate_server.domain.modification.enums.RequestTargetType;
import com.paymate.paymate_server.domain.modification.enums.RequestType;
import com.paymate.paymate_server.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "modification_requests")
public class ModificationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester; // 요청한 알바생

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private RequestTargetType targetType; // ATTENDANCE(출퇴근) / SCHEDULE(스케줄)

    @Column(name = "target_id")
    private Long targetId; // 해당 근태/스케줄의 ID 번호 (등록 요청일 경우 null일 수도 있음)

    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue; // 변경 전 (JSON 문자열 추천)

    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue; // 변경 후 (JSON 문자열 추천)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason; // 사유

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status; // PENDING(대기) / APPROVED(승인) / REJECTED(거절)

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate; // 대상 날짜

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private RequestType requestType; // REGISTER / UPDATE / DELETE

    // 👇 [추가] 언제 요청했는지 알아야 최신순 정렬 가능
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 👇 [추가] 언제 승인/거절 처리되었는지 기록
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ [비즈니스 로직] 상태 변경 편의 메서드
    public void updateStatus(RequestStatus status) {
        this.status = status;
    }
}