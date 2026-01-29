package com.paymate.paymate_server.domain.store.repository;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Employment;
import com.paymate.paymate_server.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional; // 🌟 이거 꼭 필요합니다!

public interface EmploymentRepository extends JpaRepository<Employment, Long> {

    // 이미 가입한 알바생인지 확인용 (기존 코드)
    boolean existsByEmployeeAndStore(User employee, Store store);

    // 🌟 [추가] 유저 아이디로 고용 정보 찾기
    // (엔티티 변수명이 employee라서 findByEmployee_Id로 해야 합니다!)
    Optional<Employment> findByEmployee_Id(Long userId);
}