package com.paymate.paymate_server.domain.schedule.repository;

import com.paymate.paymate_server.domain.member.entity.User; // [추가] User import 필요
import com.paymate.paymate_server.domain.schedule.entity.Schedule;
import com.paymate.paymate_server.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional; // [추가] Optional import 필요

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // 1. 월간 조회
    @Query("SELECT s FROM Schedule s WHERE s.store.id = :storeId AND MONTH(s.workDate) = :month AND YEAR(s.workDate) = :year")
    List<Schedule> findMonthlySchedule(@Param("storeId") Long storeId, @Param("year") int year, @Param("month") int month);

    // 2. 주간 조회
    List<Schedule> findByStoreAndWorkDateBetween(Store store, LocalDate startDate, LocalDate endDate);

    // 3. 내 스케줄 조회
    @Query("SELECT s FROM Schedule s WHERE s.user.id = :userId AND s.workDate BETWEEN :startDate AND :endDate")
    List<Schedule> findMyWeeklySchedule(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // =================================================================
    // ▼ [NEW] 지각 체크용 (특정 유저의 특정 날짜 스케줄 조회)
    // =================================================================
    Optional<Schedule> findByUserAndStoreAndWorkDate(User user, Store store, LocalDate workDate);
}