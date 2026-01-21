package com.paymate.paymate_server.domain.salary.repository;

import com.paymate.paymate_server.domain.salary.entity.SalaryPayment;
import com.paymate.paymate_server.domain.salary.enums.PaymentStatus;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, Long> {

    // 1. 알바생: 본인의 전체 급여 내역 조회 (최신순)
    List<SalaryPayment> findAllByUserOrderByPeriodStartDesc(User user);

    // 2. 사장님: 해당 월의 매장 전체 급여 현황 조회 (미정산 건 상단 노출 로직은 Service에서 처리 가능)
    @Query("SELECT s FROM SalaryPayment s WHERE s.store.id = :storeId " +
            "AND s.periodStart >= :startDate AND s.periodEnd <= :endDate")
    List<SalaryPayment> findAllByStoreAndPeriod(@Param("storeId") Long storeId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM SalaryPayment s WHERE s.store.id = :storeId " +
            "AND YEAR(s.periodStart) = :year " +
            "AND MONTH(s.periodStart) = :month")
    List<SalaryPayment> findAllByStoreIdAndYearAndMonth(@Param("storeId") Long storeId,
                                                        @Param("year") int year,
                                                        @Param("month") int month);

    // 3. 중복 생성 방지: 특정 유저의 특정 기간 급여 데이터가 이미 존재하는지 확인
    Optional<SalaryPayment> findByUserAndStoreAndPeriodStart(User user, Store store, LocalDate periodStart);

    // 4. 사장님: 특정 상태(예: REQUESTED)인 급여 목록만 조회
    List<SalaryPayment> findAllByStoreAndStatus(Store store, PaymentStatus status);
}