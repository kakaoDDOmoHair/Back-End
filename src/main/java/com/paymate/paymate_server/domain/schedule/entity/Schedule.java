package com.paymate.paymate_server.domain.schedule.entity;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    // ▼ [수정 핵심] 기존 worker -> user로 이름 변경!
    // 이렇게 해야 서비스에서 .user()와 .getUser()를 쓸 수 있습니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate workDate;

    private LocalTime startTime;

    private LocalTime endTime;

    // ▼ [추가] 서비스 코드(updateSchedule)에서 사용하는 수정 메서드
    public void updateTime(LocalDate workDate, LocalTime startTime, LocalTime endTime) {
        this.workDate = workDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public void updateStartTime(LocalTime newStartTime) {
        this.startTime = newStartTime;
    }
    public void updateEndTime(LocalTime newEndTime) {
        this.endTime = newEndTime;
    }
}