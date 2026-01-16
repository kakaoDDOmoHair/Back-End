package com.paymate.paymate_server.domain.attendance.entity;

import com.paymate.paymate_server.domain.attendance.enums.AttendanceStatus;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 알바생

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store; // 매장

    @Column(name = "clock_in")
    private LocalDateTime clockIn; // 출근 시간

    @Column(name = "clock_out")
    private LocalDateTime clockOut; // 퇴근 시간

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status; // 정상, 지각, 조퇴

    @Column(name = "work_date")
    private LocalDate workDate;

    @Column(name = "break_time")
    private Integer breakTime; // 휴게시간 (분)
}