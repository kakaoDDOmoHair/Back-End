package com.paymate.paymate_server.domain.modification.dto;

import com.paymate.paymate_server.domain.modification.entity.ModificationRequest;
import com.paymate.paymate_server.domain.modification.enums.RequestStatus;
import com.paymate.paymate_server.domain.modification.enums.RequestTargetType;
import com.paymate.paymate_server.domain.modification.enums.RequestType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ModificationResponseDto {
    private Long requestId;
    private Long requesterId;
    private String requesterName; // 알바생 이름 (편의상 추가)
    private RequestTargetType targetType;
    private RequestType requestType;
    private RequestStatus status;
    private String beforeValue;
    private String afterValue;
    private String reason;
    private LocalDateTime createdAt;

    public ModificationResponseDto(ModificationRequest request) {
        this.requestId = request.getId();
        this.requesterId = request.getRequester().getId();
        this.requesterName = request.getRequester().getName(); // User 엔티티에 getName() 있다고 가정
        this.targetType = request.getTargetType();
        this.requestType = request.getRequestType();
        this.status = request.getStatus();
        this.beforeValue = request.getBeforeValue();
        this.afterValue = request.getAfterValue();
        this.reason = request.getReason();
        this.createdAt = request.getCreatedAt();
    }
}