package com.paymate.paymate_server.global.util;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.enums.UserRole;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final MemberRepository userRepository;

    @Override
    public void run(String... args) {
        // 유저가 한 명도 없으면 1명 생성 (사장님)
        if (userRepository.count() == 0) {
            User owner = User.builder()
                    .name("김도홍")
                    .password("1234") // 👈 이 줄을 꼭 추가해주세요! (비밀번호 필수)
                    .email("owner@paymate.com")
                    .role(UserRole.OWNER) // 👈 이 줄 추가! (사장님이니까 OWNER)
                    // 필요한 다른 필수 필드가 있다면 여기에 추가 (예: password, role 등)
                    .build();
            userRepository.save(owner);
            System.out.println("=============================================");
            System.out.println("====== [TEST] 임시 사장님(ID:1) 생성 완료 ======");
            System.out.println("=============================================");
        }
    }
}
