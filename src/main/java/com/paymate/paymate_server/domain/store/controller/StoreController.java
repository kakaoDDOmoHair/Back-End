package com.paymate.paymate_server.domain.store.controller;

import com.paymate.paymate_server.domain.store.dto.*;
import com.paymate.paymate_server.domain.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // 1. 매장 최종 등록 API
    @PostMapping
    public ResponseEntity<String> registerStore(@RequestBody StoreRequest request) {
        Long storeId = storeService.createStore(request);
        return ResponseEntity.ok("매장 등록 성공! ID: " + storeId);
    }

    // 2. 매장 상세 조회 API
    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getStoreInfo(@PathVariable Long storeId) {
        StoreResponse response = storeService.getStoreDetail(storeId);
        return ResponseEntity.ok(response);
    }

    // 3. 사업자 번호 검증 API
    @GetMapping("/check-business")
    public ResponseEntity<CheckBusinessResponse> checkBusinessNumber(@RequestParam("businessNumber") String businessNumber) {
        CheckBusinessResponse response = storeService.validateBusinessNumber(businessNumber);
        return ResponseEntity.ok(response);
    }

    // 4. 대시보드 통계 조회 API
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(@RequestParam("storeId") Long storeId) {
        DashboardResponse response = storeService.getStoreDashboard(storeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join")
    public ResponseEntity<JoinResponse> joinStore(@RequestBody JoinRequest request) {

        // 🕵️‍♂️ [감시 카메라] 들어오는 값을 눈으로 확인해봅시다!
        System.out.println("======================================");
        System.out.println("🚨 프론트에서 보낸 ID: " + request.getUserId());
        System.out.println("🚨 프론트에서 보낸 코드: " + request.getInviteCode());
        System.out.println("======================================");

        Long storeId = storeService.joinStore(request);
        return ResponseEntity.ok(new JoinResponse("매장 가입 성공", storeId));
    }

}