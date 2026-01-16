package com.paymate.paymate_server.domain.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "work_log")
public class WorkLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    // 출퇴근 기록과 1:1 연결
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;

    @Column(name = "total_hours")
    private Float totalHours;

    @Column(name = "overtime_hours")
    private Float overtimeHours;

    @Column(name = "night_hours")
    private Float nightHours;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "daily_wage")
    private Integer dailyWage;
}