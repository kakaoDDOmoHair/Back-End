package com.paymate.paymate_server.domain.manual.dto;

import com.paymate.paymate_server.domain.manual.enums.ManualCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ManualRequest {
    private Long storeId;
    private String title;
    private String content;
    private ManualCategory category; // "OPEN", "CLOSE" 등으로 받음
}