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

    // ▼▼▼ [이 메서드가 없어서 에러가 났었습니다! 추가됨] ▼▼▼
    // 해당 유저와 매장의 계약서 중 근무 시작일이 가장 빠른(최초 계약) 1건 조회
    Optional<Contract> findTopByUserAndStoreOrderByWorkStartDateAsc(User user, Store store);
}