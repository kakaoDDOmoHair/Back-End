package com.paymate.paymate_server.domain.schedule.entity;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User worker;

    @Column(nullable = false)
    private LocalDate workDate; // 근무 날짜 (YYYY-MM-DD)

    @Column(nullable = false)
    private LocalTime startTime; // 시작 시간 (HH:mm)

    @Column(nullable = false)
    private LocalTime endTime;   // 종료 시간 (HH:mm)

    // 편의 메서드: 시간 변경
    public void updateTime(LocalDate date, LocalTime start, LocalTime end) {
        this.workDate = date;
        this.startTime = start;
        this.endTime = end;
    }
}