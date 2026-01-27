package com.paymate.paymate_server.domain.manual.dto;

import com.paymate.paymate_server.domain.manual.enums.ManualCategory;
import lombok.Data; // 👈 Getter 대신 Data 사용
import lombok.NoArgsConstructor;

@Data // 👈 여기를 수정하세요! (@Getter -> @Data)
@NoArgsConstructor
public class ManualRequest {
    private Long storeId;
    private String title;
    private String content;
    private ManualCategory category;
}