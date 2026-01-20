package com.paymate.paymate_server.domain.manual.repository;

import com.paymate.paymate_server.domain.manual.entity.Manual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManualRepository extends JpaRepository<Manual, Long> {
    // 매장별 목록 조회 (최신 수정일 순)
    List<Manual> findAllByStoreIdOrderByUpdatedAtDesc(Long storeId);
}