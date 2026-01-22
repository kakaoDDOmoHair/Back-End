package com.paymate.paymate_server.domain.notification.enums;

public enum NotificationType {

    // 1. 공지사항 (전체 공지 등)
    NOTICE,

    // 2. 스케줄/근무 관련 (스케줄 배정, 근무 변경 등)
    WORK,

    // 3. 출퇴근 관련 (지각 알림, 출근/퇴근 체크 완료)
    ATTENDANCE,

    // 4. 급여/정산 관련 (월급날 임박, 정산 완료, 급여 명세서 도착)
    // 기존 PAYMENT 대신 급여라는 의미가 더 명확한 PAYROLL 추천
    PAYMENT,

    // 5. 요청/승인 관련 (정정 요청 들어옴, 요청 승인됨/거절됨)
    // ★ 아까 만든 ModificationService에서 사용!
    REQUEST,

    // 6. 전자계약 관련 (근로계약서 도착, 서명 완료)
    CONTRACT
}