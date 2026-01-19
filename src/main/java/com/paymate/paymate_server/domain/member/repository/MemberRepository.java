package com.paymate.paymate_server.domain.member.repository;

import com.paymate.paymate_server.domain.member.entity.User; // 사용자님의 Entity 이름이 User라면 이것을 임포트
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<User, Long> {

    // 이메일로 중복 가입 여부를 확인하거나 로그인할 때 사용합니다.
    Optional<User> findByEmail(String email);

}