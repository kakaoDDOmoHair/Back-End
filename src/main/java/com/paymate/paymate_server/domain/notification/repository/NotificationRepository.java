package com.paymate.paymate_server.domain.notification.repository;

import com.paymate.paymate_server.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 기존 메서드: 내 알림 목록 조회
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // [추가 1] 안 읽은 알림 개수 세기 (count)
    long countByUserIdAndIsReadFalse(Long userId);

    // [추가 2] 안 읽은 알림 목록만 가져오기 (전체 읽음 처리용)
    List<Notification> findAllByUserIdAndIsReadFalse(Long userId);
}