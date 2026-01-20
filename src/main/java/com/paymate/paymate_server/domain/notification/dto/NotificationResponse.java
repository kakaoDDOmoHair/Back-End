package com.paymate.paymate_server.domain.notification.dto;

import com.paymate.paymate_server.domain.notification.entity.Notification;
import com.paymate.paymate_server.domain.notification.enums.NotificationType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private LocalDateTime createdAt;
    // ❌ relatedId 삭제: 프론트에서 이동 안 할 거니까 필요 없음

    public NotificationResponse(Notification notification) {
        this.id = notification.getId();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.type = notification.getType();
        this.isRead = notification.isRead();
        this.createdAt = notification.getCreatedAt();
    }
}