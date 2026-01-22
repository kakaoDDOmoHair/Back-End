package com.paymate.paymate_server.domain.member.entity;

import com.paymate.paymate_server.domain.member.enums.UserRole;
import com.paymate.paymate_server.domain.member.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate; // [추가] 날짜 타입을 위해 필요
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

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    // --- [추가된 필드 시작] ---
    @Column(name = "birth_date", length = 6)
    private String birthDate; // 생년월일 (예: "980101")

    // --- [추가된 필드 끝] ---

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