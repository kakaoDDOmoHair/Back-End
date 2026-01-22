package com.paymate.paymate_server.domain.member.entity;

import com.paymate.paymate_server.domain.member.enums.UserRole;
import com.paymate.paymate_server.domain.member.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    // --- [핵심 추가] 로그인용 아이디 ---
    // unique = true: 아이디는 중복될 수 없음
    @Column(nullable = false, unique = true, length = 30)
    private String username;

    // 이메일은 이제 '로그인용'이 아니라 '연락처/알림용'으로 사용
    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "birth_date", length = 6)
    private String birthDate; // 생년월일 (예: "980101")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role; // OWNER(사장), WORKER(알바)

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "bank_name", length = 20)
    private String bankName;

    @Column(name = "hourly_wage")
    private Integer hourlyWage;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Account> accounts = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }
}