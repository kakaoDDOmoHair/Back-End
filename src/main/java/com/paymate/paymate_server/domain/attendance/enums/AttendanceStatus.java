package com.paymate.paymate_server.domain.attendance.enums;

public enum AttendanceStatus {
    ON,      // 근무 중 (출근)
    OFF,     // 퇴근 완료
    LATE,    // 지각 (나중에 확장용)
    ABSENT,  // 결근
    PENDING  // 승인 대기 (수동 등록 시)
}