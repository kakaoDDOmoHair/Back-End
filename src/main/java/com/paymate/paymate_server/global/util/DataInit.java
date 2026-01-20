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
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;

    @Override
    public void run(String... args) {

        // 1. 임시 사장님 생성
        User owner = memberRepository.findByEmail("owner@paymate.com").orElse(null);
        if (owner == null) {
            owner = User.builder()
                    .name("김사장")
                    .email("owner@paymate.com")
                    .password("1234")
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
                    .password("1234")
                    .role(UserRole.WORKER)
                    .build();
            memberRepository.save(worker);
            System.out.println("✅ [DataInit] 임시 알바생(ID:2) 생성 완료");
        }
    }
}