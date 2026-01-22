package com.paymate.paymate_server.domain.salary.service;

import com.paymate.paymate_server.domain.attendance.entity.Attendance;
import com.paymate.paymate_server.domain.attendance.repository.AttendanceRepository;
import com.paymate.paymate_server.domain.member.entity.Account;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.AccountRepository;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.notification.enums.NotificationType;
import com.paymate.paymate_server.domain.notification.service.NotificationService; // 👈 서비스 Import
import com.paymate.paymate_server.domain.salary.dto.SalaryDto;
import com.paymate.paymate_server.domain.salary.entity.SalaryPayment;
import com.paymate.paymate_server.domain.salary.enums.PaymentStatus;
import com.paymate.paymate_server.domain.salary.repository.SalaryPaymentRepository;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import com.paymate.paymate_server.global.util.AesUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SalaryService {

    private final SalaryPaymentRepository salaryPaymentRepository;
    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;
    private final AesUtil aesUtil;
    private final AccountRepository accountRepository;
    // NotificationRepository 제거됨
    private final NotificationService notificationService; // 👈 알림 서비스(FCM 포함) 사용

    /**
     * 사장님용: 알바생별 정산 실행 및 완료 처리
     */
    public void processPayment(Long paymentId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 건을 찾을 수 없습니다."));

        User worker = payment.getUser();

        System.out.println("=== 입금 실행 (Simulation) ===");
        System.out.println("대상: " + worker.getName());
        System.out.println("금액: " + payment.getTotalAmount() + "원");

        payment.completePayment();
        sendPayslipEmail(payment.getId());

        // 🔔 [수정됨] 급여 입금 알림 (DB저장 + 푸시발송)
        notificationService.send(
                worker,
                NotificationType.PAYMENT,
                "급여 입금 완료 💰",
                String.format("%s 매장에서 급여(%d원)가 입금되었습니다.", payment.getStore().getName(), payment.getTotalAmount())
        );
    }

    /**
     * 계좌 정보 조회 (복호화 적용)
     */
    @Transactional(readOnly = true)
    public SalaryDto.AccountResponse getAccountInfo(Long paymentId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));

        Account account = payment.getAccount();

        if (account == null) {
            return SalaryDto.AccountResponse.builder()
                    .bank("정보 없음").account("계좌가 연결되지 않았습니다").holder(payment.getUser().getName()).build();
        }

        String decryptedAccount;
        try {
            decryptedAccount = aesUtil.decrypt(account.getAccountNumber());
        } catch (Exception e) {
            decryptedAccount = account.getAccountNumber();
        }

        return SalaryDto.AccountResponse.builder()
                .bank(account.getBankName())
                .account(decryptedAccount)
                .holder(payment.getUser().getName())
                .build();
    }

    @Transactional
    public String completePayment(Long paymentId, Long accountId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("이미 정산 완료된 내역입니다.");
        }

        User worker = payment.getUser();
        Account targetAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌 정보 없음"));

        if (!targetAccount.getUser().getId().equals(worker.getId())) {
            throw new IllegalArgumentException("이 계좌는 해당 알바생의 계좌가 아닙니다.");
        }

        long amount = payment.getTotalAmount();
        targetAccount.deposit(amount);

        payment.completePayment();

        // 🔔 [수정됨] 급여 입금 알림
        notificationService.send(
                worker,
                NotificationType.PAYMENT,
                "급여 입금 완료 💰",
                String.format("급여 %d원이 입금되었습니다. (잔액: %d원)", amount, targetAccount.getBalance())
        );

        return String.format("[기존내역 확정] %s님께 %d원 입금 완료!", worker.getName(), amount);
    }

    // 3. 실시간 예상 급여 조회 (알바생)
    @Transactional(readOnly = true)
    public SalaryDto.EstimatedResponse getEstimatedSalary(Long storeId, Long userId, int year, int month) {
        User user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Integer hourlyWage = (user.getHourlyWage() != null && user.getHourlyWage() > 0) ? user.getHourlyWage() : 9860;

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Attendance> attendances = attendanceRepository.findAllByUserAndCheckInTimeBetween(
                user, start.atStartOfDay(), end.atTime(23, 59, 59));

        double totalHours = attendances.stream().mapToDouble(Attendance::calculateTotalHours).sum();

        long rawAmount = Math.round(totalHours * hourlyWage);
        long tax = Math.round(rawAmount * 0.033);
        long finalAmount = rawAmount - tax;

        return SalaryDto.EstimatedResponse.builder()
                .period(start.toString() + " ~ " + LocalDate.now().toString())
                .amount(finalAmount)
                .totalHours(totalHours)
                .build();
    }

    // 4. 정산 요청하기 (알바생)
    public void requestPayment(Long paymentId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));

        payment.requestSalary();
    }

    // 5. 월별 급여 목록 조회 (사장님용)
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlySalaryList(Long storeId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<SalaryPayment> payments = salaryPaymentRepository.findAllByStoreAndPeriod(storeId, start, end);
        long totalAmount = payments.stream().mapToLong(SalaryPayment::getTotalAmount).sum();

        List<SalaryDto.MonthlyResponse> list = payments.stream().map(p -> SalaryDto.MonthlyResponse.builder()
                .name(p.getUser().getName())
                .amount(p.getTotalAmount())
                .status(p.getStatus().toString())
                .build()).collect(Collectors.toList());

        return Map.of("totalAmount", totalAmount, "employeeCount", list.size(), "payments", list);
    }

    // 1. 급여 내역 조회 (알바생용 리스트)
    @Transactional(readOnly = true)
    public List<SalaryDto.HistoryResponse> getSalaryHistory(Long userId) {
        User user = memberRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        List<SalaryPayment> payments = salaryPaymentRepository.findAllByUserOrderByPeriodStartDesc(user);

        return payments.stream().map(p -> SalaryDto.HistoryResponse.builder()
                .id(p.getId())
                .month(p.getPeriodStart().getMonthValue() + "월")
                .amount(p.getTotalAmount())
                .status(p.getStatus().toString())
                .build()).collect(Collectors.toList());
    }

    // 2. 명세서 이메일 발송
    @Async
    public void sendPayslipEmail(Long paymentId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));
        System.out.println("이메일 발송 완료: " + payment.getUser().getEmail());
    }

    // 3. 급여대장 엑셀 다운로드
    public void generateSalaryExcel(Long storeId, int year, int month, HttpServletResponse response) throws IOException {
        List<SalaryPayment> payments = salaryPaymentRepository.findAllByStoreIdAndYearAndMonth(storeId, year, month);
        Store store = storeRepository.findById(storeId).orElseThrow();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(year + "년 " + month + "월 급여대장");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("성명");
        headerRow.createCell(1).setCellValue("지급액");
        headerRow.createCell(2).setCellValue("정산상태");
        headerRow.createCell(3).setCellValue("정산일자");

        int rowIdx = 1;
        for (SalaryPayment payment : payments) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(payment.getUser().getName());
            row.createCell(1).setCellValue(payment.getTotalAmount());
            row.createCell(2).setCellValue(payment.getStatus().toString());
            row.createCell(3).setCellValue(payment.getCreatedAt().toString());
        }

        String fileName = year + "년" + month + "월_급여대장_" + store.getName();
        String encodedFileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + ".xlsx\"; filename*=UTF-8''" + encodedFileName + ".xlsx");

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // [신규] 즉시 정산 및 이체 실행
    @Transactional
    public String executeNewPayment(Long storeId, Long userId, Long accountId, int year, int month) {
        User worker = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("알바생 정보를 찾을 수 없습니다."));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("매장 정보를 찾을 수 없습니다."));
        Account targetAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌 정보를 찾을 수 없습니다."));

        SalaryDto.EstimatedResponse estimate = getEstimatedSalary(storeId, userId, year, month);

        SalaryPayment newPayment = SalaryPayment.builder()
                .user(worker)
                .store(store)
                .account(targetAccount)
                .totalAmount(estimate.getAmount())
                .totalHours(estimate.getTotalHours())
                .periodStart(LocalDate.of(year, month, 1))
                .periodEnd(LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth()))
                .status(PaymentStatus.WAITING)
                .build();

        targetAccount.deposit(estimate.getAmount());
        newPayment.completePayment();
        salaryPaymentRepository.save(newPayment);

        // 🔔 [수정됨] 급여 입금 알림
        notificationService.send(
                worker,
                NotificationType.PAYMENT,
                "급여 입금 완료 💰",
                String.format("%d월 급여 %d원이 입금되었습니다. (잔액: %d원)",
                        month, estimate.getAmount(), targetAccount.getBalance())
        );

        String displayAccount;
        try {
            displayAccount = aesUtil.decrypt(targetAccount.getAccountNumber());
        } catch (Exception e) {
            displayAccount = targetAccount.getAccountNumber();
        }

        return String.format("[%s] %s님께 %d원 정산 완료! (계좌: %s, 잔액: %d원)",
                store.getName(), worker.getName(), estimate.getAmount(), displayAccount, targetAccount.getBalance());
    }
}