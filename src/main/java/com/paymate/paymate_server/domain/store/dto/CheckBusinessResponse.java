package com.paymate.paymate_server.domain.store.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckBusinessResponse {
    private boolean isValid;
    private String status; // "ACTIVE"(정상), "INACTIVE"(휴폐업) 등
}