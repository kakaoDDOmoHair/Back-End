package com.paymate.paymate_server.domain.manual.dto;

import com.paymate.paymate_server.domain.manual.entity.Manual;
import com.paymate.paymate_server.domain.manual.enums.ManualCategory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ManualResponse {
    private Long manualId;
    private Long storeId;
    private String title;
    private String content;
    private ManualCategory category;
    private LocalDateTime updatedAt;

    public ManualResponse(Manual manual) {
        this.manualId = manual.getId();
        this.storeId = manual.getStore().getId();
        this.title = manual.getTitle();
        this.content = manual.getContent();
        this.category = manual.getCategory();
        this.updatedAt = manual.getUpdatedAt();
    }
}