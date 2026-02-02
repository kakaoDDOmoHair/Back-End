package com.paymate.paymate_server.domain.notification.controller;

import com.paymate.paymate_server.domain.notification.dto.NotificationResponse;
import com.paymate.paymate_server.domain.notification.service.NotificationService;
import com.paymate.paymate_server.global.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 로그인 유저 ID 추출 (null이면 401)
    private Long getUserIdOrNull(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return userDetails != null ? userDetails.getId() : null;
    }

    // 1. 알림 목록 조회
    @GetMapping
    public ResponseEntity<?> getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = getUserIdOrNull(userDetails);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        List<NotificationResponse> notifications = notificationService.getMyNotifications(userId);
        return ResponseEntity.ok(Map.of("status", "success", "data", notifications));
    }

    // 2. 개별 알림 읽음 처리
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> readNotification(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = getUserIdOrNull(userDetails);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        notificationService.readNotification(id, userId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "알림 읽음 처리 완료"));
    }

    // 3. 안 읽은 알림 개수 (뱃지) — 5초마다 호출, 캐시 없이 매 요청마다 DB 조회
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = getUserIdOrNull(userDetails);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("status", "success", "data", Map.of("count", count)));
    }

    // 4. 전체 읽음 처리
    @PatchMapping("/read-all")
    public ResponseEntity<?> readAllNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = getUserIdOrNull(userDetails);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        notificationService.readAllNotifications(userId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "전체 읽음 처리 완료"));
    }
}