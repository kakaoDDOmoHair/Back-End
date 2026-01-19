package com.paymate.paymate_server.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AccountVerifyRequestDto {
    private String bankCode;
    private String accountNumber;
    private String ownerName;
}