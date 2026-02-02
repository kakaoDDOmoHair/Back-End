package com.paymate.paymate_server.domain.attendance.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AttendanceDto {

    // 1. 출근 Request
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClockInRequest {
        private Long storeId;
        private Long userId;
        private Double lat;
        private Double lon;
        private String wifiBssid;
    }

    // 2. 출근 Response
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ClockInResponse {
        private boolean success;
        private Long attendanceId;
        private String status;
        private LocalDateTime clockInTime;
    }

    // 3. 퇴근 Request
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClockOutRequest {
        private Long attendanceId; // 또는 userId로 조회 가능
        private Double lat;
        private Double lon;
    }

    // 4. 퇴근 Response
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ClockOutResponse {
        private boolean success;
        private String message;
        private LocalDateTime clockOutTime;
        private Double totalHours;
    }

    // 5. 월간/일간 조회 Response (공통 List Item)
    // startTime, endTime: ISO 8601 + 타임존 (KST 예: "2025-01-31T09:39:00+09:00") — "HH:mm"만 보내지 않음
    @Getter
    @Builder
    @AllArgsConstructor
    public static class AttendanceLog {
        private Long attendanceId;
        private Long userId;
        private String name;
        private String workDate;
        private String storeName;
        /** 출근 시각 (ISO 8601 KST). 퇴근 전이면 endTime만 null */
        private String startTime;
        /** 퇴근 시각 (ISO 8601 KST). 아직 퇴근 전이면 null */
        private String endTime;
        /** 표시용 "HH:mm~HH:mm" 또는 "HH:mm~" (등록/기록 시간 컬럼용) */
        private String time;
        private String status;
    }

    // 6. 직접 수정 Request
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifyRequest {
        private String workDate; // "2026-01-10"
        private String startTime; // "12:00"
        private String endTime;   // "18:00"
        private String status;    // "NORMAL" -> Enum 매핑 필요
    }

    // 7. 실시간 근무 현황 (사장님용) Response
    @Getter
    @Builder
    @AllArgsConstructor
    public static class TodayResponse {
        private Double totalTime;
        private Long totalWage;
        private Map<String, Double> summary;
        private List<AttendanceLog> list;    // 상세 리스트
    }

    // 8. 일별 근무 기록 조회 Response (프론트 daily 목록·오늘 상태 표시용)
    // startTime, endTime: ISO 8601 + 타임존 (KST). 퇴근 전이면 endTime null
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyLog {
        private Long attendanceId;
        private Long userId;
        private String name;
        private String startTime;  // ISO 8601 KST
        private String endTime;   // ISO 8601 KST, 퇴근 전이면 null
        /** 표시용 "HH:mm~HH:mm" 또는 "HH:mm~" (기록 시간 컬럼용) */
        private String time;
        private Long wage;
        private String status;
    }

    // 9. 근무 기록 직접 등록 Request
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManualRegisterRequest {
        private Long storeId;
        private Long userId;
        private String workDate;
        private String startTime;
        private String endTime;
    }

    // 10. 수정 요청 Request
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorrectionRequest {
        private Long attendanceId;
        private String targetDate;
        private String requestType;
        private String beforeTime;
        private String afterTime; // "17:00 ~ 22:30"
        private String reason;
    }

    // 11. 수정 요청 승인/거절 Request
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestProcess {
        private String status; // APPROVED or REJECTED
    }

}