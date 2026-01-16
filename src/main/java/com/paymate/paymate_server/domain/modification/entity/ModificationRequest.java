package com.paymate.paymate_server.domain.modification.entity;

import com.paymate.paymate_server.domain.modification.enums.RequestStatus;
import com.paymate.paymate_server.domain.modification.enums.RequestTargetType;
import com.paymate.paymate_server.domain.modification.enums.RequestType;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

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
    @Column(name = "target_type")
    private RequestTargetType targetType; // 근태인지 스케줄인지

    @Column(name = "target_id")
    private Long targetId; // 해당 근태/스케줄의 ID 번호

    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue;

    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    private RequestStatus status; // 대기/승인/거절

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type")
    private RequestType requestType;
}