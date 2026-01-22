package com.paymate.paymate_server.domain.notification.scheduler;

import com.paymate.paymate_server.domain.notification.enums.NotificationType;
import com.paymate.paymate_server.domain.notification.service.NotificationService; // 👈 서비스 Import
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollNotificationScheduler {

    // NotificationRepository 제거됨
    private final NotificationService notificationService; // 👈 알림 서비스(FCM 포함) 사용
    private final StoreRepository storeRepository;

    /**
     * ⏰ 급여 알림 발송 스케줄러
     */
    // [운영용] 매일 아침 09:00:00 실행
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendPayrollReminders() {
        log.info("⏰ [Scheduler] 급여 정산일 임박 체크 시작...");

        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);
        int targetDay = threeDaysLater.getDayOfMonth();

        List<Store> stores = storeRepository.findAll();
        int count = 0;

        for (Store store : stores) {
            if (store.getPayDay() == targetDay) {

                // 🔔 [수정됨] 알림 생성 및 저장 (PAYMENT 타입) + 푸시 발송
                notificationService.send(
                        store.getOwner(),
                        NotificationType.PAYMENT,
                        "급여 정산 임박",
                        String.format("사장님, 3일 뒤(%s)는 급여 정산일입니다. 잊지 말고 챙겨주세요! 💸", threeDaysLater)
                );

                count++;
                log.info("🔔 [알림 발송] 매장: {}, 사장님: {}", store.getName(), store.getOwner().getName());
            }
        }

        if (count == 0) {
            log.info(" - 알림 대상 매장이 없습니다. (3일 뒤가 월급날인 매장 없음)");
        }
    }
}