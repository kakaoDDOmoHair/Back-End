package com.paymate.paymate_server.domain.store.entity;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User employee; // 알바생

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;   // 매장

    @Enumerated(EnumType.STRING)
    private UserRole role; // 역할 (WORKER 등)

    private LocalDateTime joinedAt; // 가입일
}