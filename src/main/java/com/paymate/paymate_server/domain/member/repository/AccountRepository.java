package com.paymate.paymate_server.domain.member.repository;

import com.paymate.paymate_server.domain.member.entity.Account;
import com.paymate.paymate_server.domain.member.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    // 유저 정보를 통해 등록된 계좌 리스트를 가져오는 메서드
    List<Account> findByUser(User user);
}