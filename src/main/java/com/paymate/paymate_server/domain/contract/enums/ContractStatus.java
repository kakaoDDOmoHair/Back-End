package com.paymate.paymate_server.domain.contract.enums;

public enum ContractStatus {
    DRAFT,      // 작성 중 (초안/OCR 인식 직후)
    REQUESTED,  // 서명 요청됨 (사장님 -> 알바생)
    ACTIVE,     // 체결 완료 (유효한 계약서)
    ENDED,      // 계약 종료
    REJECTED    // 반려됨
}