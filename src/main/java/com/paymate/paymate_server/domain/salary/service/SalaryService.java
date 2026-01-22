package com.paymate.paymate_server.domain.salary.service;

import com.lowagie.text.pdf.BaseFont;
import com.paymate.paymate_server.domain.attendance.entity.Attendance;
import com.paymate.paymate_server.domain.attendance.repository.AttendanceRepository;
import com.paymate.paymate_server.domain.contract.entity.Contract;
import com.paymate.paymate_server.domain.contract.repository.ContractRepository;
import com.paymate.paymate_server.domain.member.entity.Account;
import com.paymate.paymate_server.domain.member.entity.User;
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
    private final ContractRepository contractRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationService notificationService;

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

        payment.completePayment();
        sendPayslipEmail(payment.getId());

        notificationService.send(
                payment.getUser(),
                NotificationType.PAYMENT,
                "급여 입금 완료 💰",
                String.format("%s 매장에서 급여(%d원)가 입금되었습니다.", payment.getStore().getName(), payment.getTotalAmount())
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

        notificationService.send(
                worker,
                NotificationType.PAYMENT,
                "급여 입금 완료 💰",
                String.format("급여 %d원이 입금되었습니다. (잔액: %d원)", amount, targetAccount.getBalance())
        );

        return String.format("[기존내역 확정] %s님께 %d원 입금 완료! (잔액: %d원)", worker.getName(), amount, targetAccount.getBalance());
    }

    @Transactional(readOnly = true)
    public SalaryDto.EstimatedResponse getEstimatedSalary(Long storeId, Long userId, int year, int month) {
        User user = memberRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Integer hourlyWage = (user.getHourlyWage() != null && user.getHourlyWage() > 0) ? user.getHourlyWage() : 9860;

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<Attendance> attendances = attendanceRepository.findAllByUserAndCheckInTimeBetween(user, start.atStartOfDay(), end.atTime(23, 59, 59));

        double totalHours = attendances.stream().mapToDouble(Attendance::calculateTotalHours).sum();
        long baseAmount = Math.round(totalHours * hourlyWage);
        long weeklyAllowance = calculateWeeklyHolidayAllowance(attendances, hourlyWage);
        long rawAmount = baseAmount + weeklyAllowance;
        long tax = Math.round(rawAmount * 0.033);
        long finalAmount = rawAmount - tax;

        return SalaryDto.EstimatedResponse.builder()
                .period(start.toString() + " ~ " + LocalDate.now().toString())
                .amount(finalAmount)
                .totalHours(totalHours)
                .build();
    }

    public void requestPayment(Long paymentId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));
        payment.requestSalary();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlySalaryList(Long storeId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<SalaryPayment> payments = salaryPaymentRepository.findAllByStoreAndPeriod(storeId, start, end);
        long totalAmount = payments.stream().mapToLong(SalaryPayment::getTotalAmount).sum();
        List<SalaryDto.MonthlyResponse> list = payments.stream().map(p -> SalaryDto.MonthlyResponse.builder()
                .name(p.getUser().getName()).amount(p.getTotalAmount()).status(p.getStatus().toString()).build()).collect(Collectors.toList());
        return Map.of("totalAmount", totalAmount, "employeeCount", list.size(), "payments", list);
    }

    @Transactional(readOnly = true)
    public List<SalaryDto.HistoryResponse> getSalaryHistory(Long userId) {
        User user = memberRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        List<SalaryPayment> payments = salaryPaymentRepository.findAllByUserOrderByPeriodStartDesc(user);
        return payments.stream().map(p -> SalaryDto.HistoryResponse.builder()
                .id(p.getId()).month(p.getPeriodStart().getMonthValue() + "월").amount(p.getTotalAmount()).status(p.getStatus().toString()).build()).collect(Collectors.toList());
    }

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

        SalaryDto.EstimatedResponse estimate = getEstimatedSalary(storeId, userId, year, month);

        SalaryPayment newPayment = SalaryPayment.builder()
                .user(worker).store(store).account(targetAccount)
                .totalAmount(estimate.getAmount()).totalHours(estimate.getTotalHours())
                .periodStart(LocalDate.of(year, month, 1))
                .periodEnd(LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth()))
                .status(PaymentStatus.WAITING).build();

        targetAccount.deposit(estimate.getAmount());
        newPayment.completePayment();
        salaryPaymentRepository.save(newPayment);

        notificationService.send(
                worker,
                NotificationType.PAYMENT,
                "급여 입금 완료 💰",
                String.format("%d월 급여 %d원이 입금되었습니다. (잔액: %d원)", month, estimate.getAmount(), targetAccount.getBalance())
        );

        String displayAccount;
        try { displayAccount = aesUtil.decrypt(targetAccount.getAccountNumber()); } catch (Exception e) { displayAccount = targetAccount.getAccountNumber(); }
        return String.format("[%s] %s님께 %d원 정산 완료! (계좌: %s, 잔액: %d원)", store.getName(), worker.getName(), estimate.getAmount(), displayAccount, targetAccount.getBalance());
    }

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