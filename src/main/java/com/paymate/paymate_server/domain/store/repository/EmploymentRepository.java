package com.paymate.paymate_server.domain.store.repository;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.enums.UserRole;
import com.paymate.paymate_server.domain.store.entity.Employment;
import com.paymate.paymate_server.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmploymentRepository extends JpaRepository<Employment, Long> {

    // 이미 가입한 알바생인지 확인용 (기존 코드)
    boolean existsByEmployeeAndStore(User employee, Store store);

    // 유저 아이디로 고용 정보 찾기
    Optional<Employment> findByEmployee_Id(Long userId);

    /**
     * 매장별·역할별 소속 인원 조회 (급여 월별 목록 등에서 "등록된 알바생" 목록용).
     * User.store_id 없이 Employment만 있는 알바생도 포함됨.
     */
    List<Employment> findByStore_IdAndRole(Long storeId, UserRole role);
}