package com.paymate.paymate_server.domain.modification.repository;

import com.paymate.paymate_server.domain.modification.entity.ModificationRequest;
import com.paymate.paymate_server.domain.modification.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModificationRepository extends JpaRepository<ModificationRequest, Long> {

    // 1. 사장님용: 매장의 모든 요청 조회 (최신순)
    List<ModificationRequest> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    // 2. 사장님용: 매장의 특정 상태(예: 대기중) 요청만 조회
    List<ModificationRequest> findByStoreIdAndStatusOrderByCreatedAtDesc(Long storeId, RequestStatus status);

    // 3. 알바생용: 내 요청 목록 조회 (최신순)
    List<ModificationRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
}