package com.paymate.paymate_server.domain.member.repository;

import com.paymate.paymate_server.domain.member.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<User, Long> {

    // 1. 이메일로 찾기 (기존 유지: 로그인/중복체크용)
    Optional<User> findByEmail(String email);

    // 2. [추가] 아이디로 찾기 (나중에 로그인용으로 사용)
    Optional<User> findByUsername(String username);

    // 3. [추가] 아이디 중복 확인용 (회원가입 시 필수)
    boolean existsByUsername(String username);
}