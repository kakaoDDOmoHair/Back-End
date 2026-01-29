package com.paymate.paymate_server.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.paymate.paymate_server.domain.schedule.enums.ScheduleRequestStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

public class ScheduleDto {

    // 1. 근무 스케줄 등록 Request (String으로 통일하여 parse 에러 방지)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        private Long storeId;
        private Long userId;
        private LocalDate workDate;
        private String startTime; // "09:00"
        private String endTime;   // "18:00"
        private String breakTime; // "60"
    }

    // 2. 등록 응답 Response
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateResponse {
        private Long scheduleId;
        private String status; // "ASSIGNED"
    }

    // 3. 월간 조회 Response
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyResponse {
        private String date;
        private Long userId;
        private String name;
        private String time;
    }

    // 4. 수정 요청 Request (알바생 -> 사장님)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModificationRequest {
        private Long scheduleId;
        private LocalDate targetDate;
        private String requestType;
        private String beforeTime;
        private String afterTime;
        private String reason;
    }

    // 5. 수정 요청 처리 Request
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HandleRequest {
        private ScheduleRequestStatus status;
    }

    // 6. 주간 조회 Response (사장님용)
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WeeklyResponse {
        private String day;
        private String time;
        private List<WorkerInfo> workers;

        @Getter
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class WorkerInfo {
            private Long scheduleId;
            private String name;
            private Integer breakTime; // 결과값은 숫자로 반환
        }
    }

    // 7. 내 스케줄 조회 Response (알바생용)
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyWeeklyResponse {
        private LocalDate date;
        private String startTime;
        private String endTime;
        private Integer breakTime;
    }

    // 8. 사장님 직접 수정 Request (🌟 에러 해결 포인트)
    @Getter // 👈 이게 없어서 getStartTime() 에러가 났던 것입니다!
    @NoArgsConstructor // 👈 JSON 파싱을 위해 필수
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private LocalDate workDate;
        private String startTime;
        private String endTime;
        private String breakTime;
    }
}