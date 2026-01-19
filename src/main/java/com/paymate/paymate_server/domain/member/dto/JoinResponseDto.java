package com.paymate.paymate_server.domain.member.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자
@AllArgsConstructor // 모든 필드 생성자 (@Builder 사용 시 필수)

public class JoinResponseDto {
    private Long userId;
    private String message;
}