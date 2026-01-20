package com.paymate.paymate_server.domain.notification.repository;

import com.paymate.paymate_server.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 알림 목록 최신순 조회 (User 엔티티 필드명이 'user'이므로 findByUserId... 사용)
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}