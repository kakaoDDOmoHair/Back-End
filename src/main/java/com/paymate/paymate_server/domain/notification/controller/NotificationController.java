package com.paymate.paymate_server.domain.notification.controller;

import com.paymate.paymate_server.domain.notification.dto.NotificationResponse;
import com.paymate.paymate_server.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController // 👈 이게 핵심! (나 컨트롤러야!)
@RequestMapping("/api/v1/notifications") // 👈 주소 설정
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 1. 알림 목록 조회
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications() {
        // TODO: 실제로는 SecurityUtil.getCurrentUserId() 사용
        Long userId = 2L; // 테스트용: 알바생 ID

        List<NotificationResponse> notifications = notificationService.getMyNotifications(userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", notifications
        ));
    }

    // 2. 알림 읽음 처리
    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> readNotification(@PathVariable Long id) {
        // TODO: 실제로는 SecurityUtil.getCurrentUserId() 사용
        Long userId = 2L; // 테스트용: 알바생 ID

        notificationService.readNotification(id, userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "알림 읽음 처리 완료"
        ));
    }
}