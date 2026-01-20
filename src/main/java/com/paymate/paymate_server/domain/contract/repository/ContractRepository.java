package com.paymate.paymate_server.domain.contract.repository;

import com.paymate.paymate_server.domain.contract.entity.Contract;
import com.paymate.paymate_server.domain.contract.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    // 1. 사장님용: 매장 ID로 계약서 목록 조회 (수정됨: c.store.storeId -> c.store.id)
    @Query("SELECT c FROM Contract c WHERE c.store.id = :storeId AND (:status IS NULL OR c.status = :status)")
    Page<Contract> findByStoreId(@Param("storeId") Long storeId,
                                 @Param("status") ContractStatus status,
                                 Pageable pageable);

    // 2. 알바생용: 유저 ID로 계약서 목록 조회
    @Query("SELECT c FROM Contract c WHERE c.user.id = :userId AND (:status IS NULL OR c.status = :status)")
    Page<Contract> findByUserId(@Param("userId") Long userId,
                                @Param("status") ContractStatus status,
                                Pageable pageable);
}