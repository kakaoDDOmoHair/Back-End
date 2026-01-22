package com.paymate.paymate_server.domain.notification.controller;

import com.paymate.paymate_server.domain.notification.dto.NotificationResponse;
import com.paymate.paymate_server.domain.notification.service.NotificationService;
// import com.paymate.paymate_server.global.security.UserDetailsImpl; // 시큐리티 설정에 따라 다름
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 1. 알림 목록 조회
    @GetMapping
    public ResponseEntity<?> getNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        // 로그인한 유저의 ID를 가져오기 (UserDetails 구현체에 따라 다를 수 있음. 일단 2L로 테스트하셔도 무방)
        // Long userId = ((UserDetailsImpl) userDetails).getUserId();
        Long userId = 2L; // 일단 테스트 유지

        List<NotificationResponse> notifications = notificationService.getMyNotifications(userId);
        return ResponseEntity.ok(Map.of("status", "success", "data", notifications));
    }

    // 2. 개별 알림 읽음 처리
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> readNotification(@PathVariable Long id) {
        Long userId = 2L;
        notificationService.readNotification(id, userId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "알림 읽음 처리 완료"));
    }

    // 3. 안 읽은 알림 개수 (뱃지)
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        Long userId = 2L;
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("status", "success", "data", Map.of("count", count)));
    }

    // 4. 전체 읽음 처리
    @PatchMapping("/read-all")
    public ResponseEntity<?> readAllNotifications() {
        Long userId = 2L;
        notificationService.readAllNotifications(userId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "전체 읽음 처리 완료"));
    }
}