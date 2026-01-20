package com.paymate.paymate_server.global.util;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.enums.UserRole;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.enums.StorePayRule; // 👈 import 확인
import com.paymate.paymate_server.domain.store.enums.TaxType;     // 👈 import 확인
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.paymate.paymate_server.domain.notification.repository.NotificationRepository;

@Component
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder; // 👈 이 한 줄 추가! (Import 필수)

    @Override
    public void run(String... args) {

        // 1. 임시 사장님 생성
        User owner = memberRepository.findByEmail("owner@paymate.com").orElse(null);
        if (owner == null) {
            owner = User.builder()
                    .name("김사장")
                    .email("owner@paymate.com")
                    .password(passwordEncoder.encode("1234"))
                    .role(UserRole.OWNER)
                    .build();
            memberRepository.save(owner);
            System.out.println("✅ [DataInit] 임시 사장님(ID:1) 생성 완료");
        }

        // 2. 임시 매장 생성
        if (storeRepository.count() == 0) {
            Store store = Store.builder()
                    .owner(owner)
                    .name("GS25 제주대점")
                    .category("CONVENIENCE_STORE") // 👈 여기는 String (파일 안 만드셔도 됨!)
                    .address("제주시 제주대학로 102")
                    .detailAddress("1층")
                    .businessNumber("123-45-67890")
                    .payDay(10)
                    .taxType(TaxType.GENERAL)       // 👈 만드신 Enum 사용!
                    .payRule(StorePayRule.MONTHLY)  // 👈 만드신 Enum 사용!
                    .build();

            storeRepository.save(store);
            System.out.println("✅ [DataInit] 임시 매장(ID:1) 자동 생성 완료!");
        }

        // 3. 임시 알바생 생성
        if (memberRepository.findByEmail("worker@paymate.com").isEmpty()) {
            User worker = User.builder()
                    .name("이알바")
                    .email("worker@paymate.com")
                    .password(passwordEncoder.encode("1234"))
                    .role(UserRole.WORKER)
                    .build();
            memberRepository.save(worker);
            System.out.println("✅ [DataInit] 임시 알바생(ID:2) 생성 완료");
        }
        // 5. 임시 알림 생성 (알바생용)
        if (notificationRepository.count() == 0) {
            User worker = memberRepository.findById(2L).orElse(null);

            if (worker != null) {
                // 1) 근로계약서 알림
                notificationRepository.save(com.paymate.paymate_server.domain.notification.entity.Notification.builder()
                        .user(worker)
                        .title("근로계약서 도착")
                        .message("근로계약서가 작성되었습니다. 확인해주세요.")
                        .type(com.paymate.paymate_server.domain.notification.enums.NotificationType.WORK)
                        .build());

                // 2) 급여 알림
                notificationRepository.save(com.paymate.paymate_server.domain.notification.entity.Notification.builder()
                        .user(worker)
                        .title("급여 정산 완료")
                        .message("1월 급여 정산이 완료되었습니다.")
                        .type(com.paymate.paymate_server.domain.notification.enums.NotificationType.PAYMENT)
                        .isRead(true) // 읽은 상태 테스트
                        .build());

                System.out.println("✅ [DataInit] 임시 알림(이동 없음) 생성 완료");
            }
        }
    }
}