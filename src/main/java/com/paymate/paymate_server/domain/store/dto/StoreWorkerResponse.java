package com.paymate.paymate_server.domain.store.dto;

import com.paymate.paymate_server.domain.member.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장 소속 알바생 목록 조회용 DTO (목록 항목 1건)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreWorkerResponse {

    private Long userId;
    private String username;
    private String name;
    private String email;
    private String role;

    public static StoreWorkerResponse from(User user) {
        if (user == null) return null;
        return StoreWorkerResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }
}
