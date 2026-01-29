package com.paymate.paymate_server.domain.salary.service;

import com.lowagie.text.pdf.BaseFont;
import com.paymate.paymate_server.domain.attendance.entity.Attendance;
import com.paymate.paymate_server.domain.attendance.repository.AttendanceRepository;
import com.paymate.paymate_server.domain.contract.entity.Contract;
import com.paymate.paymate_server.domain.contract.repository.ContractRepository;
import com.paymate.paymate_server.domain.member.entity.Account;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.enums.UserRole;
import com.paymate.paymate_server.domain.member.repository.AccountRepository;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.notification.enums.NotificationType;
import com.paymate.paymate_server.domain.notification.service.NotificationService;
import com.paymate.paymate_server.domain.salary.dto.SalaryDto;
import com.paymate.paymate_server.domain.salary.entity.SalaryPayment;
import com.paymate.paymate_server.domain.salary.enums.PaymentStatus;
import com.paymate.paymate_server.domain.salary.repository.SalaryPaymentRepository;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import com.paymate.paymate_server.global.util.AesUtil;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    // [추가] 고급 기능(PDF, 메일, 알림)을 위한 의존성
    private final ContractRepository contractRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationService notificationService;

    // [추가] 주휴수당 계산 로직
    private long calculateWeeklyHolidayAllowance(List<Attendance> attendances, int hourlyWage) {
        Map<Integer, Double> weeklyHours = attendances.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCheckInTime().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                        Collectors.summingDouble(Attendance::calculateTotalHours)
                ));

        long totalAllowance = 0;
        for (double hours : weeklyHours.values()) {
            if (hours >= 15.0) {
                double effectiveHours = Math.min(hours, 40.0);
                totalAllowance += Math.round((effectiveHours / 40.0) * 8.0 * hourlyWage);
            }
        }
        return totalAllowance;
    }

    public void processPayment(Long paymentId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 건을 찾을 수 없습니다."));

        payment.completePayment(); // 상태 변경

        // [업그레이드] 실제 이메일 발송
        sendPayslipEmail(payment.getId());

        // [추가] 알림 발송
        notificationService.send(
                payment.getUser(),
                NotificationType.PAYMENT,
                "급여 정산 완료",
                String.format("%s 매장의 급여 정산이 완료되었습니다.", payment.getStore().getName())
        );
    }

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
        try { decryptedAccount = aesUtil.decrypt(account.getAccountNumber()); }
        catch (Exception e) { decryptedAccount = account.getAccountNumber(); }
        return SalaryDto.AccountResponse.builder().bank(account.getBankName()).account(decryptedAccount).holder(payment.getUser().getName()).build();
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

        // [추가] 알림 발송
        notificationService.send(
                worker,
                NotificationType.PAYMENT,
                "급여 입금 완료 💰",
                String.format("급여 %d원이 입금되었습니다.", amount)
        );

        return String.format("[기존내역 확정] %s님께 %d원 입금 완료!", worker.getName(), amount);
    }

    // [수정] 주휴수당 및 세금 포함 계산으로 업그레이드
    @Transactional(readOnly = true)
    public SalaryDto.EstimatedResponse getEstimatedSalary(Long storeId, Long userId, int year, int month) {
        User user = memberRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Integer hourlyWage = (user.getHourlyWage() != null && user.getHourlyWage() > 0) ? user.getHourlyWage() : 9860;

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<Attendance> attendances = attendanceRepository.findAllByUserAndCheckInTimeBetween(user, start.atStartOfDay(), end.atTime(23, 59, 59));

        double totalHours = attendances.stream().mapToDouble(Attendance::calculateTotalHours).sum();

        // 상세 계산 적용
        long baseAmount = Math.round(totalHours * hourlyWage);
        long weeklyAllowance = calculateWeeklyHolidayAllowance(attendances, hourlyWage);
        long rawAmount = baseAmount + weeklyAllowance;
        long tax = Math.round(rawAmount * 0.033);
        long finalAmount = rawAmount - tax;

        return SalaryDto.EstimatedResponse.builder()
                .period(start.toString() + " ~ " + LocalDate.now().toString())
                .amount(finalAmount)
                .totalHours(totalHours)
                // 추가 필드 채우기
                .baseSalary(baseAmount)
                .weeklyAllowance(weeklyAllowance)
                .tax(tax)
                .build();
    }

    public void requestPayment(Long paymentId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));
        payment.requestSalary();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlySalaryList(Long storeId, int year, int month) {
        // 1. 매장에 소속된 모든 '알바생(WORKER)' 조회 (사장님 본인 제외)
        List<User> workers = memberRepository.findByStoreIdAndRole(storeId, UserRole.WORKER);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // 2. 이미 해당 월에 생성된 정산 내역 조회
        List<SalaryPayment> existingPayments = salaryPaymentRepository.findAllByStoreAndPeriod(storeId, start, end);

        // 3. 전체 알바생 목록을 기준으로 DTO 생성
        List<SalaryDto.MonthlyResponse> list = workers.stream().map(worker -> {
            // 이 알바생의 이번 달 정산 데이터가 이미 생성되었는지 확인
            Optional<SalaryPayment> paymentOpt = existingPayments.stream()
                    .filter(p -> p.getUser().getId().equals(worker.getId()))
                    .findFirst();

            return SalaryDto.MonthlyResponse.builder()
                    .name(worker.getName())
                    // 정산 내역이 있으면 그 금액, 없으면 아직 0원
                    .amount(paymentOpt.map(SalaryPayment::getTotalAmount).orElse(0L))
                    // 정산 내역 유무에 따른 상태 표시
                    .status(paymentOpt.map(p -> p.getStatus().toString()).orElse("NOT_STARTED"))
                    .userId(worker.getId())
                    // 아까 동기화 성공한 유저의 accountId 사용
                    .accountId(worker.getAccountId() != null ? Long.valueOf(worker.getAccountId()) : null)
                    .build();
        }).collect(Collectors.toList());

        long totalAmount = existingPayments.stream().mapToLong(SalaryPayment::getTotalAmount).sum();

        return Map.of(
                "totalAmount", totalAmount,
                "employeeCount", list.size(),
                "payments", list
        );
    }

    @Transactional(readOnly = true)
    public List<SalaryDto.HistoryResponse> getSalaryHistory(Long userId) {
        User user = memberRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        List<SalaryPayment> payments = salaryPaymentRepository.findAllByUserOrderByPeriodStartDesc(user);
        return payments.stream().map(p -> SalaryDto.HistoryResponse.builder()
                .id(p.getId()).month(p.getPeriodStart().getMonthValue() + "월").amount(p.getTotalAmount()).status(p.getStatus().toString()).build()).collect(Collectors.toList());
    }

    // [업그레이드] 실제 PDF 생성 및 이메일 전송 로직 적용
    @Async
    public void sendPayslipEmail(Long paymentId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));

        try {
            SalaryDto.EstimatedResponse detail = getPayslipPreview(paymentId);
            Context context = new Context();

            LocalDate joinDate = payment.getUser().getCreatedAt().toLocalDate();
            Optional<Contract> contract = contractRepository.findTopByUserAndStoreOrderByWorkStartDateAsc(
                    payment.getUser(), payment.getStore());
            if (contract.isPresent()) {
                joinDate = contract.get().getWorkStartDate();
            }
            context.setVariable("joinDate", joinDate);
            context.setVariable("birthDate", payment.getUser().getBirthDate() != null ? payment.getUser().getBirthDate() : "-");
            context.setVariable("workerName", payment.getUser().getName());
            context.setVariable("storeName", payment.getStore().getName());
            context.setVariable("year", payment.getPeriodStart().getYear());
            context.setVariable("month", payment.getPeriodStart().getMonthValue());
            context.setVariable("totalAmount", detail.getAmount());
            context.setVariable("baseSalary", detail.getBaseSalary());
            context.setVariable("weeklyAllowance", detail.getWeeklyAllowance());
            context.setVariable("tax", detail.getTax());
            context.setVariable("totalHours", detail.getTotalHours());
            context.setVariable("hourlyWage", (payment.getUser().getHourlyWage() != null) ? payment.getUser().getHourlyWage() : 9860);

            String html = templateEngine.process("payslip-template", context);

            byte[] pdfBytes;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ITextRenderer renderer = new ITextRenderer();
                String fontPath = "C:/Windows/Fonts/malgun.ttf";
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    renderer.getFontResolver().addFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
                renderer.setDocumentFromString(html);
                renderer.layout();
                renderer.createPDF(baos);
                pdfBytes = baos.toByteArray();
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(payment.getUser().getEmail());
            helper.setSubject("[PayMate] " + payment.getPeriodStart().getMonthValue() + "월 임금명세서");
            helper.setText("안녕하세요. " + payment.getStore().getName() + "입니다. 요청하신 임금명세서를 보내드립니다.", false);
            helper.addAttachment("임금명세서_" + payment.getUser().getName() + ".pdf", new ByteArrayResource(pdfBytes));
            mailSender.send(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateSalaryExcel(Long storeId, int year, int month, HttpServletResponse response) throws IOException {
        List<SalaryPayment> payments = salaryPaymentRepository.findAllByStoreIdAndYearAndMonth(storeId, year, month);
        Store store = storeRepository.findById(storeId).orElseThrow();
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(year + "년 " + month + "월 급여대장");
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("성명"); headerRow.createCell(1).setCellValue("지급액");
        headerRow.createCell(2).setCellValue("정산상태"); headerRow.createCell(3).setCellValue("정산일자");
        int rowIdx = 1;
        for (SalaryPayment payment : payments) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(payment.getUser().getName());
            row.createCell(1).setCellValue(payment.getTotalAmount());
            row.createCell(2).setCellValue(payment.getStatus().toString());
            row.createCell(3).setCellValue(payment.getCreatedAt().toString());
        }
        String encodedFileName = UriUtils.encode(year + "년" + month + "월_급여대장_" + store.getName(), StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + ".xlsx\"; filename*=UTF-8''" + encodedFileName + ".xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @Transactional
    public String executeNewPayment(Long storeId, Long userId, Long accountId, int year, int month) {
        User worker = memberRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("알바생 정보를 찾을 수 없습니다."));
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new IllegalArgumentException("매장 정보를 찾을 수 없습니다."));
        Account targetAccount = accountRepository.findById(accountId).orElseThrow(() -> new IllegalArgumentException("계좌 정보를 찾을 수 없습니다."));

        // 1. 예상 급여 및 근무 시간 계산 결과 가져오기
        SalaryDto.EstimatedResponse estimate = getEstimatedSalary(storeId, userId, year, month);

        // 🌟 [핵심 추가] 근무 기록(총 시간)이 0이면 정산 중단
        if (estimate.getTotalHours() == null || estimate.getTotalHours() <= 0) {
            throw new IllegalStateException(String.format("%s님은 해당 월의 근무 기록이 없어 정산을 진행할 수 없습니다.", worker.getName()));
        }

        // 2. 기록이 있는 경우에만 아래 로직 실행
        SalaryPayment newPayment = SalaryPayment.builder()
                .user(worker).store(store).account(targetAccount)
                .totalAmount(estimate.getAmount()).totalHours(estimate.getTotalHours())
                .periodStart(LocalDate.of(year, month, 1))
                .periodEnd(LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth()))
                .status(PaymentStatus.WAITING).build();

        targetAccount.deposit(estimate.getAmount());
        newPayment.completePayment(); // 상태를 COMPLETED로 변경
        salaryPaymentRepository.save(newPayment);

        // 알림 발송
        notificationService.send(
                worker,
                NotificationType.PAYMENT,
                "급여 입금 완료 💰",
                String.format("%d월 급여 %d원이 입금되었습니다.", month, estimate.getAmount())
        );

        String displayAccount;
        try { displayAccount = aesUtil.decrypt(targetAccount.getAccountNumber()); } catch (Exception e) { displayAccount = targetAccount.getAccountNumber(); }

        return String.format("[%s] %s님께 %d원 정산 완료! (계좌: %s, 잔액: %d원)",
                store.getName(), worker.getName(), estimate.getAmount(), displayAccount, targetAccount.getBalance());
    }

    // [추가] 명세서 데이터 미리보기 메서드
    @Transactional(readOnly = true)
    public SalaryDto.EstimatedResponse getPayslipPreview(Long paymentId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역을 찾을 수 없습니다."));

        User user = payment.getUser();
        int hourlyWage = (user.getHourlyWage() != null && user.getHourlyWage() > 0) ? user.getHourlyWage() : 9860;

        long baseSalary = Math.round(payment.getTotalHours() * hourlyWage);
        long tax = Math.round((payment.getTotalAmount() / 0.967) * 0.033);
        long weeklyAllowance = payment.getTotalAmount() + tax - baseSalary;

        return SalaryDto.EstimatedResponse.builder()
                .period(payment.getPeriodStart() + " ~ " + payment.getPeriodEnd())
                .totalHours(payment.getTotalHours())
                .baseSalary(baseSalary)
                .weeklyAllowance(Math.max(0, weeklyAllowance))
                .tax(tax)
                .amount(payment.getTotalAmount())
                .build();
    }

    // [추가] HTML 미리보기 메서드
    @Transactional(readOnly = true)
    public String getPayslipHtmlPreview(Long paymentId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));

        SalaryDto.EstimatedResponse detail = getPayslipPreview(paymentId);
        Context context = new Context();

        LocalDate joinDate = payment.getUser().getCreatedAt().toLocalDate();
        Optional<Contract> contract = contractRepository.findTopByUserAndStoreOrderByWorkStartDateAsc(payment.getUser(), payment.getStore());
        if (contract.isPresent()) joinDate = contract.get().getWorkStartDate();

        context.setVariable("joinDate", joinDate);
        context.setVariable("birthDate", payment.getUser().getBirthDate() != null ? payment.getUser().getBirthDate() : "-");
        context.setVariable("workerName", payment.getUser().getName());
        context.setVariable("storeName", payment.getStore().getName());
        context.setVariable("year", payment.getPeriodStart().getYear());
        context.setVariable("month", payment.getPeriodStart().getMonthValue());
        context.setVariable("totalAmount", detail.getAmount());
        context.setVariable("baseSalary", detail.getBaseSalary());
        context.setVariable("weeklyAllowance", detail.getWeeklyAllowance());
        context.setVariable("tax", detail.getTax());
        context.setVariable("totalHours", detail.getTotalHours());
        context.setVariable("hourlyWage", (payment.getUser().getHourlyWage() != null) ? payment.getUser().getHourlyWage() : 9860);

        return templateEngine.process("payslip-template", context);
    }
}