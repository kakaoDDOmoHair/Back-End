package com.paymate.paymate_server.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.paymate.paymate_server.domain.member.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    public void sendPush(User user, String title, String body) {
        String token = user.getFcmToken();

        // 토큰이 없으면 (앱에 로그인 안 한 유저 등) 발송 패스
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            // 1. 메시지 구성 (수신자 토큰 + 제목 + 내용)
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            // 2. 구글로 전송!
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("🚀 [FCM 전송 성공] To: {}, Response: {}", user.getName(), response);

        } catch (Exception e) {
            log.error("🔥 [FCM 전송 실패] 에러: {}", e.getMessage());
        }
    }
}