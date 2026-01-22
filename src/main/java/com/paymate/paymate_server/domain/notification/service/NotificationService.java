package com.paymate.paymate_server.domain.notification.service;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.notification.dto.NotificationResponse;
import com.paymate.paymate_server.domain.notification.entity.Notification;
import com.paymate.paymate_server.domain.notification.enums.NotificationType;
import com.paymate.paymate_server.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FcmService fcmService; // 👈 1. FCM 서비스 다시 추가!

    // 1. 통합 알림 발송 (DB 저장 + 푸시)
    // 👈 2. 파라미터 순서를 (User, Type, Title, Message)로 맞춰야 다른 서비스에서 에러가 안 납니다.
    public void send(User receiver, NotificationType type, String title, String message) {
        // (1) DB 저장
        Notification notification = Notification.builder()
                .user(receiver)
                .type(type)      // 순서 주의
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        // (2) 푸시 알림 발송 (이게 있어야 폰이 울립니다!)
        fcmService.sendPush(receiver, title, message);
    }

    // 2. 내 알림 목록 조회
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::new)
                .collect(Collectors.toList());
    }

    // 3. 알림 읽음 처리 (개별)
    public void readNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));

        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 알림만 읽을 수 있습니다.");
        }
        notification.read();
    }

    // 4. 안 읽은 알림 개수 조회 (뱃지용)
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // 5. 알림 전체 읽음 처리
    public void readAllNotifications(Long userId) {
        List<Notification> unreadList = notificationRepository.findAllByUserIdAndIsReadFalse(userId);
        for (Notification notification : unreadList) {
            notification.read();
        }
    }
}