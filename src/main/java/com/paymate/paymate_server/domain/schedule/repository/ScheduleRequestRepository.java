package com.paymate.paymate_server.domain.schedule.repository;

import com.paymate.paymate_server.domain.schedule.entity.ScheduleRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRequestRepository extends JpaRepository<ScheduleRequest, Long> {
}