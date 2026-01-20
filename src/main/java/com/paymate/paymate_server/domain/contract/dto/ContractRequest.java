package com.paymate.paymate_server.domain.contract.dto;

import com.paymate.paymate_server.domain.contract.enums.ContractStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ContractRequest {
    // 생성 시 필요
    private Long storeId;
    private Long userId;

    // 계약 내용
    private Integer wage;
    private LocalDate workStartDate;
    private LocalDate workEndDate;
    private String workHours; // 예: "09:00-18:00"

    // 파일 업로드 결과 (Mocking용)
    private String fileUrl;

    // 수정 시 상태 변경 (예: ACTIVE, REJECTED)
    private ContractStatus status;
}