package com.paymate.paymate_server.domain.verification.repository;

import com.paymate.paymate_server.domain.verification.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BankRepository extends JpaRepository<BankAccount, Long> {

    // 은행명과 계좌번호로 계좌 정보 찾기
    Optional<BankAccount> findByBankNameAndAccountNumber(String bankName, String accountNumber);
}