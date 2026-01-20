package com.paymate.paymate_server.domain.attendance.entity;

import com.paymate.paymate_server.domain.attendance.enums.AttendanceStatus;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // ▼ [수정 1] Service 코드와 이름 통일 (clockIn -> checkInTime)
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    // ▼ [추가] 위치 및 WiFi 정보 (API 명세서 반영)
    private Double lat;       // 위도
    private Double lon;       // 경도
    private String wifiBssid; // WiFi MAC 주소

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    // ▼ [유지] 기존에 쓰시던 필드 (쿼리 조회용으로 유용함)
    @Column(name = "work_date")
    private String workDate; // 날짜 검색 최적화를 위해 String (YYYY-MM-DD) 권장

    @Column(name = "break_time")
    private Integer breakTime; // 휴게시간 (분)

    // =================================================================
    // ▼ [추가] Service 로직을 위한 비즈니스 메서드 (필수!)
    // =================================================================

    // 1. 퇴근 처리 (시간, 위치 업데이트 + 상태 변경)
    public void clockOut(LocalDateTime time, Double lat, Double lon) {
        this.checkOutTime = time;
        this.status = AttendanceStatus.OFF;
        this.lat = lat;
        this.lon = lon;
    }

    // 2. 근무 시간 계산 (분 단위 -> 시간 단위 변환)
    public double calculateTotalHours() {
        if (checkInTime == null || checkOutTime == null) return 0.0;

        long minutes = Duration.between(checkInTime, checkOutTime).toMinutes();

        // 휴게 시간이 있다면 제외
        if (breakTime != null) {
            minutes -= breakTime;
        }

        // 소수점 첫째 자리까지 반올림 (예: 4.5시간)
        return Math.round((minutes / 60.0) * 10.0) / 10.0;
    }

    // 3. 관리자 직접 수정용 메서드
    public void updateInfo(LocalDateTime start, LocalDateTime end, AttendanceStatus status) {
        this.checkInTime = start;
        this.checkOutTime = end;
        this.status = status;
    }
}