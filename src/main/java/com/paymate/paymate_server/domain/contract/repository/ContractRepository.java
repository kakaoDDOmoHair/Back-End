package com.paymate.paymate_server.domain.contract.repository;

import com.paymate.paymate_server.domain.contract.entity.Contract;
import com.paymate.paymate_server.domain.contract.enums.ContractStatus;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query("SELECT c FROM Contract c WHERE c.store.id = :storeId AND (:status IS NULL OR c.status = :status)")
    Page<Contract> findByStoreId(@Param("storeId") Long storeId,
                                 @Param("status") ContractStatus status,
                                 Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.user.id = :userId AND (:status IS NULL OR c.status = :status)")
    Page<Contract> findByUserId(@Param("userId") Long userId,
                                @Param("status") ContractStatus status,
                                Pageable pageable);

    // 충돌의 원인이었던 부분! 우리가 추가한 메서드입니다.
    Optional<Contract> findTopByUserAndStoreOrderByWorkStartDateAsc(User user, Store store);
}