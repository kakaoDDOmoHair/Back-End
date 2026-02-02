package com.paymate.paymate_server.domain.modification.dto;

import com.paymate.paymate_server.domain.modification.entity.ModificationRequest;
import com.paymate.paymate_server.domain.modification.enums.RequestStatus;
import com.paymate.paymate_server.domain.modification.enums.RequestTargetType;
import com.paymate.paymate_server.domain.modification.enums.RequestType;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class ModificationResponseDto {
    private Long requestId;
    private Long storeId;
    private Long requesterId;
    private String requesterName;
    private RequestTargetType targetType;
    private Long targetId;
    private RequestType requestType;
    private LocalDate targetDate;
    private RequestStatus status;
    private String beforeValue;  // 이전 값 (HH:mm~HH:mm). API 응답에 포함 요청 필드
    private String afterValue;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 엔티티만으로 DTO 생성 (beforeValue는 엔티티 값 그대로) */
    public ModificationResponseDto(ModificationRequest request) {
        this(request, request.getBeforeValue());
    }

    /** resolvedBeforeValue: DB에 없을 때 targetId로 조회한 이전 값. 비어 있으면 엔티티 beforeValue 사용 */
    public ModificationResponseDto(ModificationRequest request, String resolvedBeforeValue) {
        this.requestId = request.getId();
        this.storeId = request.getStore() != null ? request.getStore().getId() : null;
        this.requesterId = request.getRequester().getId();
        this.requesterName = request.getRequester().getName();
        this.targetType = request.getTargetType();
        this.targetId = request.getTargetId();
        this.requestType = request.getRequestType();
        this.targetDate = request.getTargetDate();
        this.status = request.getStatus();
        this.beforeValue = (resolvedBeforeValue != null && !resolvedBeforeValue.isBlank())
                ? resolvedBeforeValue
                : request.getBeforeValue();
        this.afterValue = request.getAfterValue();
        this.reason = request.getReason();
        this.createdAt = request.getCreatedAt();
        this.updatedAt = request.getUpdatedAt();
    }
}