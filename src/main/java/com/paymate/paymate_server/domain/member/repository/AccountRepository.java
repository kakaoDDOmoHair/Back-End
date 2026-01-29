package com.paymate.paymate_server.domain.member.repository;

import com.paymate.paymate_server.domain.member.entity.Account;
import com.paymate.paymate_server.domain.member.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // 🌟 [핵심 수정]
    // 1. findFirst: 하나만 가져온다
    // 2. ByUser: 특정 유저의 데이터를
    // 3. OrderByIdDesc: ID(PK) 기준으로 내림차순 정렬한다 (높은 숫자가 위로)
    Optional<Account> findFirstByUserOrderByIdDesc(User user);
}