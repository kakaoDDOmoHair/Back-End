package com.paymate.paymate_server.domain.schedule.entity;

// ▼ 이 부분! 아까 만든 Enum을 가져와야 합니다.
import com.paymate.paymate_server.domain.schedule.enums.ScheduleRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ScheduleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "schedule_id 열을 해결할 수 없습니다" 경고가 떠도 무시하고 실행하세요!
    // 서버가 실행될 때 JPA가 알아서 DB에 이 컬럼을 만들어줍니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Enumerated(EnumType.STRING)
    private ScheduleRequestStatus status; // PENDING, APPROVED, REJECTED

    private String requestType; // "MODIFICATION"

    private String beforeTime;
    private String afterTime;
    private String reason;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    public void updateStatus(ScheduleRequestStatus status) {
        this.status = status;
        this.processedAt = LocalDateTime.now();
    }
}