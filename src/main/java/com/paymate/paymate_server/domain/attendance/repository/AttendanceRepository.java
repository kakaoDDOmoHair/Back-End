package com.paymate.paymate_server.domain.attendance.repository;

import com.paymate.paymate_server.domain.attendance.entity.Attendance;
import com.paymate.paymate_server.domain.attendance.enums.AttendanceStatus;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // 현재 출근 중인 기록 찾기 (퇴근용)
    Optional<Attendance> findByUserAndStatus(User user, AttendanceStatus status);

    // 중복 출근 방지
    boolean existsByUserAndStatus(User user, AttendanceStatus status);

    // 월간/일간 조회용 (기간 검색) — 최신 출근순 정렬
    List<Attendance> findAllByUserAndCheckInTimeBetweenOrderByCheckInTimeDesc(User user, LocalDateTime start, LocalDateTime end);

    // 매장별 일간 조회 (사장님용)
    List<Attendance> findAllByStoreAndCheckInTimeBetween(Store store, LocalDateTime start, LocalDateTime end);

    List<Attendance> findAllByStoreAndWorkDate(Store store, String workDate);
}