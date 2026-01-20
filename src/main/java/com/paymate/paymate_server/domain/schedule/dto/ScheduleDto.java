package com.paymate.paymate_server.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.paymate.paymate_server.domain.schedule.enums.ScheduleRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ScheduleDto {

    // 1. 근무 스케줄 등록 Request
    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private Long storeId;
        private Long userId;
        private LocalDate workDate;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;
    }

    // 2. 등록 응답 Response
    @Getter
    @Builder
    public static class CreateResponse {
        private Long scheduleId;
        private String status; // "ASSIGNED"
    }

    // 3. 월간 조회 Response
    @Getter
    @Builder
    public static class MonthlyResponse {
        private String date; // "2026-01-15"
        private Long userId;
        private String name;
        private String time; // "09:00~14:00"
    }

    // 4. 수정 요청 Request (알바생 -> 사장님)
    @Getter
    @NoArgsConstructor
    public static class ModificationRequest {
        private Long scheduleId;
        private LocalDate targetDate;
        private String requestType; // "MODIFICATION"
        private String beforeTime;
        private String afterTime;
        private String reason;
    }

    // 5. 수정 요청 처리 Request (사장님 승인/거절)
    @Getter
    @NoArgsConstructor
    public static class HandleRequest {
        private ScheduleRequestStatus status; // APPROVED or REJECTED
    }

    // 6. 주간 조회 Response
    @Getter
    @Builder
    public static class WeeklyResponse {
        private String day; // "MON"
        private String time; // "09:00" (시작시간)
        private List<String> names; // ["도홍", "지운"]
    }

    // 7. 내 스케줄 조회 Response
    @Getter
    @Builder
    public static class MyWeeklyResponse {
        private LocalDate date;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;
    }

    // 8. 사장님 직접 수정 Request
    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private LocalDate workDate;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;
    }
}