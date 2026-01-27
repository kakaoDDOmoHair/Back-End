package com.paymate.paymate_server.domain.verification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "verification_account") // DB 테이블 이름
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bankName;      // 은행명 (예: 신한은행)
    private String accountNumber; // 계좌번호
    private String ownerName;     // 예금주

    @Column(name = "user_id")
    private Long userId;
}