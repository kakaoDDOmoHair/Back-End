package com.paymate.paymate_server.domain.schedule.repository;

import com.paymate.paymate_server.domain.schedule.entity.Schedule;
import com.paymate.paymate_server.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // 1. 월간 조회 (연도와 월을 따로 받아서 조회)
    // 서비스에서 findMonthlySchedule(...)을 호출하므로 이 이름이 꼭 있어야 해요!
    @Query("SELECT s FROM Schedule s WHERE s.store.id = :storeId AND MONTH(s.workDate) = :month AND YEAR(s.workDate) = :year")
    List<Schedule> findMonthlySchedule(@Param("storeId") Long storeId, @Param("year") int year, @Param("month") int month);

    // 2. 주간 조회 (매장 객체로 조회)
    // 서비스에서 store 객체를 넘기므로 findByStore... 여야 합니다. (StoreId 아님)
    List<Schedule> findByStoreAndWorkDateBetween(Store store, LocalDate startDate, LocalDate endDate);

    // 3. 내 스케줄 조회 (알바생 ID로 조회)
    // Schedule 엔티티 필드명이 'user'이므로 s.user.id로 찾아야 합니다.
    @Query("SELECT s FROM Schedule s WHERE s.user.id = :userId AND s.workDate BETWEEN :startDate AND :endDate")
    List<Schedule> findMyWeeklySchedule(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}