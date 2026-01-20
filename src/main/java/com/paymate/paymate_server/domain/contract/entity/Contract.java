package com.paymate.paymate_server.domain.contract.entity;

import com.paymate.paymate_server.domain.contract.enums.ContractStatus;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Setter // 👈 ✨ 핵심! 이 줄이 있어야 setWage, setStatus 등을 쓸 수 있습니다.
@AllArgsConstructor
@Builder
@Table(name = "contracts")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    private Integer wage; // 시급

    @Column(name = "work_start_date")
    private LocalDate workStartDate;

    @Column(name = "work_end_date")
    private LocalDate workEndDate;

    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl; // 계약서 파일 경로

    @Column(name = "work_hours", length = 100)
    private String workHours; // 근로 시간 텍스트

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}