package com.paymate.paymate_server.domain.store.repository;

import com.paymate.paymate_server.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    // 기존에 있던 것
    boolean existsByBusinessNumber(String businessNumber);

    // 👇 [필수] 이 코드를 꼭 추가해주세요!
    Optional<Store> findByInviteCode(String inviteCode);
}