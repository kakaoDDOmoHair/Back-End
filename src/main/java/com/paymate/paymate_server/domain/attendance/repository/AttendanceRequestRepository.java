package com.paymate.paymate_server.domain.attendance.repository;

import com.paymate.paymate_server.domain.attendance.entity.AttendanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRequestRepository extends JpaRepository<AttendanceRequest, Long> {
}