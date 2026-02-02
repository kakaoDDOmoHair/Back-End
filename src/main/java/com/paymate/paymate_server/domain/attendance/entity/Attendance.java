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

    // 2. 근무 시간 계산 (법정 휴게시간 자동 차감 적용)
    public double calculateTotalHours() {
        if (checkInTime == null || checkOutTime == null) return 0.0;

        // 전체 체류 시간 계산 (분 단위)
        long totalMinutes = java.time.Duration.between(checkInTime, checkOutTime).toMinutes();
        double rawHours = totalMinutes / 60.0;

        // 법정 휴게시간 계산 (무급)
        // 8시간 근무(총 9시간 체류) 시 1시간, 4시간 근무(총 4.5시간 체류) 시 30분 차감
        double autoBreakMinutes = 0;
        if (rawHours >= 9.0) {
            autoBreakMinutes = 60; // 1시간
        } else if (rawHours >= 4.5) {
            autoBreakMinutes = 30; // 30분
        }

        // 실제 근무 분 계산 (전체 분 - 자동 휴게분 - 수동 breakTime 필드가 있다면 추가 차감)
        long finalMinutes = totalMinutes - (long)autoBreakMinutes;

        // 만약 엔티티에 별도의 breakTime 필드가 있다면 그것도 함께 고려 (선택 사항)
    /*
    if (this.breakTime != null) {
        finalMinutes -= this.breakTime;
    }
    */

        // 소수점 첫째 자리까지 반올림 (예: 4.5시간)
        return Math.round((Math.max(0, finalMinutes) / 60.0) * 10.0) / 10.0;
    }

    // 3. 관리자 직접 수정용 메서드 (기존 유지)
    public void updateInfo(LocalDateTime start, LocalDateTime end, AttendanceStatus status) {
        this.checkInTime = start;
        this.checkOutTime = end;
        this.status = status;
    }

    // 정정 요청 시 퇴근 '시간'만 수정하는 메서드 (기존 유지)
    public void updateEndTime(java.time.LocalTime newTime) {
        if (this.checkInTime == null) return; // 출근 기록이 없으면 수정 불가 처리

        java.time.LocalDate targetDate = (this.checkOutTime != null)
                ? this.checkOutTime.toLocalDate()
                : this.checkInTime.toLocalDate();

        this.checkOutTime = java.time.LocalDateTime.of(targetDate, newTime);
    }

    /**
     * 정정 요청 승인 시 출/퇴근 시간을 함께 수정.
     * 상태(status)나 위치 정보는 유지합니다.
     */
    public void updateTimes(LocalDateTime newCheckInTime, LocalDateTime newCheckOutTime) {
        this.checkInTime = newCheckInTime;
        this.checkOutTime = newCheckOutTime;
    }
}