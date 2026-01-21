package com.paymate.paymate_server.domain.modification.dto;

import com.paymate.paymate_server.domain.modification.enums.RequestTargetType;
import com.paymate.paymate_server.domain.modification.enums.RequestType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ModificationRequestDto {
    private Long storeId;
    private RequestTargetType targetType; // ATTENDANCE, SCHEDULE
    private Long targetId;                // 수정할 대상 ID (등록일 땐 null 가능)
    private RequestType requestType;      // REGISTER, UPDATE, DELETE
    private String beforeValue;
    private String afterValue;
    private LocalDate targetDate;
    private String reason;

}