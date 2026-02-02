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
import com.paymate.paymate_server.domain.store.entity.Employment;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.EmploymentRepository;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import com.paymate.paymate_server.global.util.AesUtil;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final EmploymentRepository employmentRepository;
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
        
        // 이미 완료된 경우 중복 처리 방지 (입금은 하지 않고 성공 메시지만 반환)
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return String.format("[이미 완료됨] %s님의 정산이 이미 완료된 상태입니다.", payment.getUser().getName());
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

    /**
     * 사장님이 정산 요청 알림을 확인했을 때 (REQUESTED → WAITING).
     * 알바생 화면에서 "확인중" 다음 단계로 넘어가도록 서버에 반영.
     */
    @Transactional
    public void acknowledgePayment(Long paymentId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));
        payment.acknowledgeByOwner();
    }

    // [수정] 주휴수당 및 세금 포함 계산으로 업그레이드
    @Transactional(readOnly = true)
    public SalaryDto.EstimatedResponse getEstimatedSalary(Long storeId, Long userId, int year, int month) {
        User user = memberRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Integer hourlyWage = (user.getHourlyWage() != null && user.getHourlyWage() > 0) ? user.getHourlyWage() : 10320;

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<Attendance> attendances = attendanceRepository.findAllByUserAndCheckInTimeBetweenOrderByCheckInTimeDesc(user, start.atStartOfDay(), end.atTime(23, 59, 59));

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

    // 알바생의 정산 요청 (paymentId가 없어도 가능 - 사장님이 정산하기 전에도 요청 가능)
    public SalaryDto.RequestResponse requestPayment(Long paymentId, Long userId, Long storeId, Integer year, Integer month) {
        SalaryPayment payment;
        
        if (paymentId != null) {
            // 기존 정산 내역이 있는 경우
            payment = salaryPaymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));
        } else {
            // 정산 내역이 없는 경우 (사장님이 아직 정산하지 않음)
            // userId, storeId, year, month로 SalaryPayment 생성
            if (userId == null || storeId == null || year == null || month == null) {
                throw new IllegalArgumentException("정산 내역이 없을 경우 userId, storeId, year, month가 필요합니다.");
            }
            
            User worker = memberRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("알바생 정보를 찾을 수 없습니다."));
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new IllegalArgumentException("매장 정보를 찾을 수 없습니다."));
            
            LocalDate periodStart = LocalDate.of(year, month, 1);
            
            // 이미 해당 기간의 정산 내역이 있는지 확인
            Optional<SalaryPayment> existingPayment = salaryPaymentRepository.findByUserAndStoreAndPeriodStart(worker, store, periodStart);
            if (existingPayment.isPresent()) {
                payment = existingPayment.get();
            } else {
                // 정산 내역이 없으면 예상 급여로 생성
                SalaryDto.EstimatedResponse estimate = getEstimatedSalary(storeId, userId, year, month);
                
                // 가장 최근 계좌 가져오기
                Account account = accountRepository.findFirstByUserOrderByIdDesc(worker)
                        .orElseThrow(() -> new IllegalArgumentException("계좌 정보가 없습니다. 계좌를 등록해주세요."));
                
                payment = SalaryPayment.builder()
                        .user(worker)
                        .store(store)
                        .account(account)
                        .totalAmount(estimate.getAmount())
                        .totalHours(estimate.getTotalHours())
                        .periodStart(periodStart)
                        .periodEnd(periodStart.withDayOfMonth(periodStart.lengthOfMonth()))
                        .status(PaymentStatus.WAITING)
                        .build();
                salaryPaymentRepository.save(payment);
            }
        }
        
        // 정산 요청 처리
        payment.requestSalary();
        
        // 🌟 [추가] 사장님에게 알림 발송
        notificationService.send(
                payment.getStore().getOwner(),
                NotificationType.PAYMENT,
                "급여 정산 요청",
                String.format("%s님이 %d월 급여 정산을 요청했습니다. (금액: %d원)", 
                        payment.getUser().getName(), 
                        payment.getPeriodStart().getMonthValue(),
                        payment.getTotalAmount())
        );
        
        // 🌟 [추가] 상세 급여 정보 계산 (응답용)
        SalaryDto.EstimatedResponse estimate = getEstimatedSalary(
                payment.getStore().getId(), 
                payment.getUser().getId(), 
                payment.getPeriodStart().getYear(),
                payment.getPeriodStart().getMonthValue()
        );
        
        // 응답 반환 (일한 시간, 요청 금액 포함)
        return SalaryDto.RequestResponse.builder()
                .paymentId(payment.getId())
                .year(payment.getPeriodStart().getYear())
                .month(payment.getPeriodStart().getMonthValue())
                .amount(payment.getTotalAmount())
                .totalHours(payment.getTotalHours() != null ? payment.getTotalHours() : estimate.getTotalHours())
                .status(payment.getStatus().toString())
                .baseSalary(estimate.getBaseSalary())
                .weeklyAllowance(estimate.getWeeklyAllowance())
                .tax(estimate.getTax())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlySalaryList(Long storeId, int year, int month) {
        // 1. 매장에 소속된 모든 '알바생(WORKER)' 조회 — Employment 기준 (등록된 알바생은 User.store_id 없어도 포함)
        List<User> workers = employmentRepository.findByStore_IdAndRole(storeId, UserRole.WORKER).stream()
                .map(Employment::getEmployee)
                .collect(Collectors.toList());

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // 2. 이미 해당 월에 생성된 정산 내역 조회
        List<SalaryPayment> existingPayments = salaryPaymentRepository.findAllByStoreAndPeriod(storeId, start, end);

        // 3. 전체 알바생 목록을 기준으로 DTO 생성 (REQUESTED일 때 requestedAt = 정산 요청 시각, KST)
        // DB/서버 시각(UTC 등)을 KST로 변환해 내려줘야 사장님 알림에 "N시간 전"이 맞게 표시됨
        ZoneId kst = ZoneId.of("Asia/Seoul");
        List<SalaryDto.MonthlyResponse> list = workers.stream().map(worker -> {
            Optional<SalaryPayment> paymentOpt = existingPayments.stream()
                    .filter(p -> p.getUser().getId().equals(worker.getId()))
                    .findFirst();

            String requestedAt = null;
            if (paymentOpt.isPresent()) {
                SalaryPayment p = paymentOpt.get();
                if (p.getStatus() == PaymentStatus.REQUESTED && p.getUpdatedAt() != null) {
                    requestedAt = p.getUpdatedAt()
                            .atZone(ZoneId.systemDefault())
                            .withZoneSameInstant(kst)
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
            }

            return SalaryDto.MonthlyResponse.builder()
                    .name(worker.getName())
                    .amount(paymentOpt.map(SalaryPayment::getTotalAmount).orElse(0L))
                    .status(paymentOpt.map(p -> p.getStatus().toString()).orElse("NOT_STARTED"))
                    .userId(worker.getId())
                    .accountId(worker.getAccountId() != null ? Long.valueOf(worker.getAccountId()) : null)
                    .paymentId(paymentOpt.map(SalaryPayment::getId).orElse(null))
                    .requestedAt(requestedAt)
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
                .id(p.getId())
                .month(p.getPeriodStart().getMonthValue() + "월")
                .amount(p.getTotalAmount())
                .totalHours(p.getTotalHours())
                .status(p.getStatus().toString())
                .build()
        ).collect(Collectors.toList());
    }

    // 알바생용 현재 월 급여 조회
    @Transactional(readOnly = true)
    public SalaryDto.CurrentMonthSalaryResponse getCurrentMonthSalary(Long userId, int year, int month) {
        User user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 알바생도 Employment를 통해 매장을 찾을 수 있어야 함 (user.getStore()는 사장님 케이스에만 있음)
        Store store = user.getStore();
        if (store == null) {
            Optional<Employment> employment = employmentRepository.findByEmployee_Id(user.getId());
            if (employment.isPresent()) {
                store = employment.get().getStore();
            }
        }
        if (store == null) {
            throw new IllegalArgumentException("소속된 매장이 없습니다.");
        }

        LocalDate periodStart = LocalDate.of(year, month, 1);
        Optional<SalaryPayment> paymentOpt = salaryPaymentRepository.findByUserAndStoreAndPeriodStart(user, store, periodStart);

        // 상세 급여 정보 계산 (기본급, 주휴수당, 세금 등)
        SalaryDto.EstimatedResponse estimate = getEstimatedSalary(store.getId(), userId, year, month);

        // SalaryPayment가 있으면 그 정보 사용, 없으면 예상 급여 정보 사용
        if (paymentOpt.isPresent()) {
            SalaryPayment payment = paymentOpt.get();
            return SalaryDto.CurrentMonthSalaryResponse.builder()
                    .paymentId(payment.getId())
                    .year(year)
                    .month(month)
                    .amount(payment.getTotalAmount())
                    .status(payment.getStatus().toString())
                    .baseSalary(estimate.getBaseSalary())
                    .weeklyAllowance(estimate.getWeeklyAllowance())
                    .tax(estimate.getTax())
                    .totalHours(payment.getTotalHours() != null ? payment.getTotalHours() : estimate.getTotalHours())
                    .build();
        } else {
            // 아직 정산이 안 된 경우 (예상 급여만 반환)
            return SalaryDto.CurrentMonthSalaryResponse.builder()
                    .paymentId(null)
                    .year(year)
                    .month(month)
                    .amount(estimate.getAmount())
                    .status("NOT_STARTED")
                    .baseSalary(estimate.getBaseSalary())
                    .weeklyAllowance(estimate.getWeeklyAllowance())
                    .tax(estimate.getTax())
                    .totalHours(estimate.getTotalHours())
                    .build();
        }
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
            context.setVariable("hourlyWage", (payment.getUser().getHourlyWage() != null) ? payment.getUser().getHourlyWage() : 10320);

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

        // 헤더 스타일
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // 본문 기본 스타일
        CellStyle bodyStyle = workbook.createCellStyle();
        bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        bodyStyle.setBorderTop(BorderStyle.THIN);
        bodyStyle.setBorderBottom(BorderStyle.THIN);
        bodyStyle.setBorderLeft(BorderStyle.THIN);
        bodyStyle.setBorderRight(BorderStyle.THIN);

        // 금액 스타일 (숫자 포맷)
        CellStyle amountStyle = workbook.createCellStyle();
        amountStyle.cloneStyleFrom(bodyStyle);
        amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

        // 헤더 행
        Row headerRow = sheet.createRow(0);
        String[] headers = {"성명", "지급액", "정산상태", "정산일자"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터 행
        int rowIdx = 1;
        for (SalaryPayment payment : payments) {
            Row row = sheet.createRow(rowIdx++);

            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(payment.getUser().getName());
            nameCell.setCellStyle(bodyStyle);

            Cell amountCell = row.createCell(1);
            amountCell.setCellValue(payment.getTotalAmount());
            amountCell.setCellStyle(amountStyle);

            Cell statusCell = row.createCell(2);
            statusCell.setCellValue(payment.getStatus().toString());
            statusCell.setCellStyle(bodyStyle);

            Cell dateCell = row.createCell(3);
            dateCell.setCellValue(payment.getCreatedAt().toString());
            dateCell.setCellStyle(bodyStyle);
        }

        // 컬럼 너비 자동 조정 + 헤더 고정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        sheet.createFreezePane(0, 1);

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
        // 🌟 [수정] WAITING 상태로 생성 (알바생이 요청할 수 있도록)
        SalaryPayment newPayment = SalaryPayment.builder()
                .user(worker).store(store).account(targetAccount)
                .totalAmount(estimate.getAmount()).totalHours(estimate.getTotalHours())
                .periodStart(LocalDate.of(year, month, 1))
                .periodEnd(LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth()))
                .status(PaymentStatus.WAITING).build();

        // 🌟 [수정] 입금 처리는 하지 않고, WAITING 상태로 저장
        // 알바생이 요청하면 REQUESTED로 변경되고, 
        // 사장님이 확인 후 completePayment()를 호출하여 COMPLETED로 변경
        salaryPaymentRepository.save(newPayment);

        // 알림 발송 (정산 내역 생성 알림)
        notificationService.send(
                worker,
                NotificationType.PAYMENT,
                "정산 내역 생성 완료",
                String.format("%d월 급여 정산 내역이 생성되었습니다. 정산 요청을 보낼 수 있습니다.", month)
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
        int hourlyWage = (user.getHourlyWage() != null && user.getHourlyWage() > 0) ? user.getHourlyWage() : 10320;

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
        context.setVariable("hourlyWage", (payment.getUser().getHourlyWage() != null) ? payment.getUser().getHourlyWage() : 10320);

        return templateEngine.process("payslip-template", context);
    }

    // [추가] 개인별 급여대장 엑셀 다운로드
    public void generateUserSalaryExcel(Long storeId, Long userId, int year, int month, HttpServletResponse response) throws IOException {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("매장 정보를 찾을 수 없습니다."));
        User user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("알바생 정보를 찾을 수 없습니다."));

        LocalDate periodStart = LocalDate.of(year, month, 1);
        Optional<SalaryPayment> paymentOpt = salaryPaymentRepository.findByUserAndStoreAndPeriodStart(user, store, periodStart);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(year + "년 " + month + "월 " + user.getName() + " 급여대장");

        // 헤더 스타일
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // 본문 기본 스타일
        CellStyle bodyStyle = workbook.createCellStyle();
        bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        bodyStyle.setBorderTop(BorderStyle.THIN);
        bodyStyle.setBorderBottom(BorderStyle.THIN);
        bodyStyle.setBorderLeft(BorderStyle.THIN);
        bodyStyle.setBorderRight(BorderStyle.THIN);

        // 금액 스타일 (숫자 포맷)
        CellStyle amountStyle = workbook.createCellStyle();
        amountStyle.cloneStyleFrom(bodyStyle);
        amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

        // 헤더 행
        Row headerRow = sheet.createRow(0);
        String[] headers = {"성명", "지급액", "정산상태", "정산일자"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터 행 (해당 알바생 1명)
        Row row = sheet.createRow(1);

        SalaryPayment payment = paymentOpt.orElse(null);
        String status = (payment != null) ? payment.getStatus().toString() : "NOT_STARTED";
        long amount = (payment != null) ? payment.getTotalAmount() : 0L;
        String createdAt = (payment != null && payment.getCreatedAt() != null)
                ? payment.getCreatedAt().toString()
                : "-";

        Cell nameCell = row.createCell(0);
        nameCell.setCellValue(user.getName());
        nameCell.setCellStyle(bodyStyle);

        Cell amountCell = row.createCell(1);
        amountCell.setCellValue(amount);
        amountCell.setCellStyle(amountStyle);

        Cell statusCell = row.createCell(2);
        statusCell.setCellValue(status);
        statusCell.setCellStyle(bodyStyle);

        Cell dateCell = row.createCell(3);
        dateCell.setCellValue(createdAt);
        dateCell.setCellStyle(bodyStyle);

        // 컬럼 너비 자동 조정 + 헤더 고정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        sheet.createFreezePane(0, 1);

        String fileName = year + "년" + month + "월_" + user.getName() + "_급여대장_" + store.getName();
        String encodedFileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + ".xlsx\"; filename*=UTF-8''" + encodedFileName + ".xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}