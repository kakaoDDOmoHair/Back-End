package com.paymate.paymate_server.domain.attendance.entity;

import com.paymate.paymate_server.domain.attendance.enums.AttendanceRequestStatus;
import com.paymate.paymate_server.domain.attendance.enums.AttendanceRequestType;
import com.paymate.paymate_server.domain.member.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// ▼ [추가] 테이블 이름을 명확하게 지정합니다.
@Table(name = "attendance_request")
public class AttendanceRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id") // ID 컬럼명도 명확하게
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id") // 외래키 컬럼명
    private Attendance attendance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 외래키 컬럼명
    private User user;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type")
    private AttendanceRequestType requestType;

    @Enumerated(EnumType.STRING)
    private AttendanceRequestStatus status;

    @Column(name = "before_time")
    private String beforeTime;

    @Column(name = "after_time")
    private String afterTime;

    private String reason;

    @Column(name = "target_start_time")
    private LocalTime targetStartTime;

    @Column(name = "target_end_time")
    private LocalTime targetEndTime;

    public void updateStatus(AttendanceRequestStatus status) {
        this.status = status;
    }
}