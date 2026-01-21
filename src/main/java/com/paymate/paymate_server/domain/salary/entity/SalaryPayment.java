package com.paymate.paymate_server.domain.salary.entity;

import com.paymate.paymate_server.domain.member.entity.Account;
import com.paymate.paymate_server.domain.salary.enums.PaymentStatus;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "salary_payments")
public class SalaryPayment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount; // 세전 급여액

    @Column(name = "total_hours")
    private Double totalHours; // 해당 기간 총 근무 시간

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate; // 실제 이체 완료 시각

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl; // 명세서 PDF 경로

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id") // DB에 account_id 컬럼 생성
    private Account account; // 👈 이 정산건에 연결된 계좌 정보
    // === 비즈니스 로직 메서드 ===

    // 알바생의 정산 요청
    public void requestSalary() {
        if (this.status != PaymentStatus.WAITING) {
            throw new IllegalStateException("정산 대기 상태일 때만 요청이 가능합니다.");
        }
        this.status = PaymentStatus.REQUESTED;
    }
    // 사장님의 이체 완료 확정
    public void completePayment() {
        // 이미 완료된 건을 또 완료할 수는 없으므로 방어 코드 추가
        if (this.status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("이미 정산이 완료된 내역입니다.");
        }

        this.status = PaymentStatus.COMPLETED;
        this.paymentDate = LocalDateTime.now(); // 실제 정산 완료 시점 기록
    }


}