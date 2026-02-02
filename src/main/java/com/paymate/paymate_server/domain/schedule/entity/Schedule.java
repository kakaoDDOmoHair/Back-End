package com.paymate.paymate_server.domain.schedule.entity;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor // JPA를 위한 기본 생성자
@AllArgsConstructor // @Builder를 위한 전체 인자 생성자
@Builder // 클래스 레벨 빌더 사용
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate workDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @Column(columnDefinition = "Integer default 0")
    private Integer breakTime;

    // ▼ [수정 핵심] 서비스 코드에서 사용하는 통합 수정 메서드
    public void updateTime(LocalDate workDate, LocalTime startTime, LocalTime endTime, Integer breakTime) {
        this.workDate = workDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.breakTime = (breakTime != null) ? breakTime : 0; // null 방어 코드
    }

    // 개별 수정이 필요한 경우를 위한 메서드들
    public void updateStartTime(LocalTime newStartTime) {
        this.startTime = newStartTime;
    }

    public void updateEndTime(LocalTime newEndTime) {
        this.endTime = newEndTime;
    }
}