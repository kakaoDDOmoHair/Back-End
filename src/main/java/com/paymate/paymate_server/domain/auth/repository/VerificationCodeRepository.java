package com.paymate.paymate_server.domain.auth.repository;

import com.paymate.paymate_server.domain.auth.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, String> {
}