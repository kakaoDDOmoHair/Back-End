package com.paymate.paymate_server.domain.verification.repository;

import com.paymate.paymate_server.domain.verification.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BankRepository extends JpaRepository<BankAccount, Long> {

    // 은행명과 계좌번호로 계좌 정보 찾기 (동일 조합이 여러 건이어도 첫 번째만 반환)
    Optional<BankAccount> findFirstByBankNameAndAccountNumber(String bankName, String accountNumber);
}