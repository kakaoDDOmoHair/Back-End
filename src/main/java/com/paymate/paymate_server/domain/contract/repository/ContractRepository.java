package com.paymate.paymate_server.domain.contract.repository;

import com.paymate.paymate_server.domain.contract.entity.Contract;
import com.paymate.paymate_server.domain.contract.enums.ContractStatus;
import com.paymate.paymate_server.domain.member.entity.User;   // [추가]
import com.paymate.paymate_server.domain.store.entity.Store;    // [추가]
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional; // [추가]

public interface ContractRepository extends JpaRepository<Contract, Long> {

    // 1. 사장님용: 매장 ID로 계약서 목록 조회
    @Query("SELECT c FROM Contract c WHERE c.store.id = :storeId AND (:status IS NULL OR c.status = :status)")
    Page<Contract> findByStoreId(@Param("storeId") Long storeId,
                                 @Param("status") ContractStatus status,
                                 Pageable pageable);

    // 2. 알바생용: 유저 ID로 계약서 목록 조회
    @Query("SELECT c FROM Contract c WHERE c.user.id = :userId AND (:status IS NULL OR c.status = :status)")
    Page<Contract> findByUserId(@Param("userId") Long userId,
                                @Param("status") ContractStatus status,
                                Pageable pageable);

    // 3. [추가됨] 급여명세서용: 해당 유저와 매장의 계약서 중 '근무 시작일'이 가장 빠른 것 1개 조회
    // (이 메서드는 SQL을 직접 안 써도, JPA가 이름만 보고 자동으로 쿼리를 만들어줍니다!)
    Optional<Contract> findTopByUserAndStoreOrderByWorkStartDateAsc(User user, Store store);
}