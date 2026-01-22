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

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

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
    // 👇 [수정 후] 이렇게 바꾸세요!
    @Builder.Default // 👈 이 어노테이션 추가
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


    // 👇 [추가] FCM 토큰 필드 (DB 컬럼으로 자동 생성됨)
    private String fcmToken;

    // 👇 [추가] 토큰 업데이트 메서드 (로그인 시 프론트가 준 토큰 저장용)
    public void updateFcmToken(String token) {
        this.fcmToken = token;}

}