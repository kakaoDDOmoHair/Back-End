package com.paymate.paymate_server.domain.store.repository;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Employment;
import com.paymate.paymate_server.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentRepository extends JpaRepository<Employment, Long> {
    // 이미 가입한 알바생인지 확인용
    boolean existsByEmployeeAndStore(User employee, Store store);
}