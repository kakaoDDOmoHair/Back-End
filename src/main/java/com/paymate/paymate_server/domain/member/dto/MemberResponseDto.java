package com.paymate.paymate_server.domain.member.dto;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponseDto {
    private Long userId;
    private Long storeId;
    private String accountId; // 🌟 [추가] 가장 최근(ID가 높은) 등록된 계좌 ID
    private String username;
    private String email;
    private String name;
    private String role;
    
    // --- 매장 정보 (출근 확인용) ---
    private StoreInfo store;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreInfo {
        private Long storeId;
        private String storeName;
        private Double latitude;  // 위도
        private Double longitude; // 경도
        private String wifiInfo;  // 와이파이 정보
    }

    /**
     * Entity -> DTO 변환 메서드
     * @param user 유저 엔티티
     * @param storeId 서비스에서 계산된 매장 ID
     * @param accountId 서비스에서 조회된 최신 계좌 ID
     * @param store 매장 엔티티 (위치/와이파이 정보 포함)
     */
    public static MemberResponseDto of(User user, Long storeId, Long accountId, Store store) {
        StoreInfo storeInfo = null;
        if (store != null) {
            storeInfo = StoreInfo.builder()
                    .storeId(store.getId())
                    .storeName(store.getName())
                    .latitude(store.getLatitude())
                    .longitude(store.getLongitude())
                    .wifiInfo(store.getWifiInfo())
                    .build();
        }
        
        return MemberResponseDto.builder()
                .userId(user.getId())
                .storeId(storeId)
                // 🌟 프론트엔드에서 "accountId": 7 형식으로 쓰기 위해 String으로 변환하여 전달
                .accountId(accountId != null ? String.valueOf(accountId) : null)
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .store(storeInfo)
                .build();
    }
}