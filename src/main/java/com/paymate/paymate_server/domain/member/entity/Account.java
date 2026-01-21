package com.paymate.paymate_server.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String bankName;      // 은행명 (예: 국민은행)
    private String accountNumber; // 암호화된 계좌번호

    @Builder.Default
    private Long balance = 0L;    // 해당 계좌의 잔액

    // 입금 메서드
    public void deposit(Long amount) {
        this.balance += amount;
    }
}