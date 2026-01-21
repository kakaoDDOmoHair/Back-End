package com.paymate.paymate_server.global.jwt;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
// ❌ import org.springframework.stereotype.Service; (삭제)

// ⚠️ [핵심] @Service, @RequiredArgsConstructor 어노테이션이 하나도 없어야 합니다!
// 그냥 쌩(?) 자바 클래스여야 스프링한테 안 들킵니다.
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    // 생성자 직접 작성
    public CustomUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return new CustomUserDetails(user);
    }
}