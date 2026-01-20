package com.paymate.paymate_server.domain.contract.dto;

import com.paymate.paymate_server.domain.contract.entity.Contract;
import lombok.Getter;

@Getter
public class ContractResponse {
    private Long contractId;
    private String storeName;
    private String workerName;
    private Integer wage;
    private String workPeriod; // "2026.01.01 ~ 2026.12.31" 형태
    private String status;
    private String fileUrl;    // 다운로드/미리보기 링크

    public ContractResponse(Contract contract) {
        this.contractId = contract.getId();
        this.storeName = contract.getStore().getName();
        this.workerName = contract.getUser().getName();
        this.wage = contract.getWage();
        this.status = contract.getStatus().name();
        this.fileUrl = contract.getFileUrl();

        // 날짜 포맷팅
        this.workPeriod = contract.getWorkStartDate() + " ~ " +
                (contract.getWorkEndDate() != null ? contract.getWorkEndDate() : "기간 없음");
    }
}