package com.paymate.paymate_server.domain.salary.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;
import com.paymate.paymate_server.domain.member.entity.Account;
import com.paymate.paymate_server.domain.member.repository.AccountRepository;
import com.paymate.paymate_server.domain.salary.enums.PaymentStatus;
import com.paymate.paymate_server.global.util.AesUtil;
import org.springframework.scheduling.annotation.Async; // @Async 해결
import jakarta.servlet.http.HttpServletResponse;      // HttpServletResponse 해결
import java.io.IOException;                           // IOException 해결
import com.paymate.paymate_server.domain.attendance.entity.Attendance;
import com.paymate.paymate_server.domain.attendance.repository.AttendanceRepository;
import com.paymate.paymate_server.domain.salary.dto.SalaryDto;
import com.paymate.paymate_server.domain.salary.entity.SalaryPayment;
import com.paymate.paymate_server.domain.salary.repository.SalaryPaymentRepository;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
// import com.paymate.paymate_server.global.util.EncryptionUtil; // 복호화 유틸 필요
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.time.LocalDate;
import java.util.List;
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

    /**
     * 사장님용: 알바생별 정산 실행 및 완료 처리
     */
    public void processPayment(Long paymentId) {
        // 1. 정산 내역 및 알바생 정보 조회
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산 건을 찾을 수 없습니다."));

        User worker = payment.getUser();

        // 2. [핵심] 등록된 계좌 정보 가져오기 (복호화)
        String bank = worker.getBankName();
        String decryptedAccount = aesUtil.decrypt(worker.getAccountNumber());
        Long amount = payment.getTotalAmount();

        // 3. 가상 은행 전송 시뮬레이션 (로그 기록)
        // 실제 뱅킹 API가 있다면 여기서 호출하겠지만, 현재는 시스템상 완료 처리가 주 목적입니다.
        System.out.println("=== 입금 실행 ===");
        System.out.println("대상: " + worker.getName());
        System.out.println("은행/계좌: " + bank + " / " + decryptedAccount);
        System.out.println("금액: " + amount + "원");

        // 4. 정산 상태 업데이트 (WAITING/REQUESTED -> COMPLETED)
        payment.completePayment(); // 엔티티 메서드: status 변경 및 paymentDate 기록

        // 5. [명세서 요건] 후속 처리: 알림 발송 및 명세서 생성 트리거
        sendPayslipEmail(payment.getId()); // 비동기 메일 발송 호출
    }

    /**
     * 1. 계좌 정보 조회 (복호화 적용)
     * 명세서: 이체 버튼 클릭 시, 알바생 계좌번호를 복호화하여 반환
     */
    @Transactional(readOnly = true)
    public SalaryDto.AccountResponse getAccountInfo(Long paymentId) {
        // 1. 정산 내역 조회
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));

        // 2. [수정 포인트] User가 아니라, Payment에 연결된 Account 엔티티를 가져옵니다.
        Account account = payment.getAccount();

        // 계좌 연결이 안 되어 있을 경우 예외 처리
        if (account == null) {
            return SalaryDto.AccountResponse.builder()
                    .bank("정보 없음")
                    .account("계좌가 연결되지 않았습니다")
                    .holder(payment.getUser().getName())
                    .build();
        }

        // 3. 복호화 로직 (Account 엔티티의 값을 사용)
        String decryptedAccount;
        try {
            decryptedAccount = aesUtil.decrypt(account.getAccountNumber());
        } catch (Exception e) {
            // 복호화 실패 시 (암호화 안 된 데이터일 경우) 원본 그대로 표시
            decryptedAccount = account.getAccountNumber();
        }

        // 4. 결과 반환
        return SalaryDto.AccountResponse.builder()
                .bank(account.getBankName())      // Account 테이블의 은행명
                .account(decryptedAccount)        // 복호화된 계좌번호
                .holder(payment.getUser().getName()) // 예금주는 알바생 이름 사용
                .build();
    }

    @Transactional
    public String completePayment(Long paymentId, Long accountId) {
        // 1. 기존에 생성되어 있던 정산 내역 조회 (WAITING 혹은 REQUESTED 상태)
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));

        // 2. 이미 완료된 건인지 체크 (중복 입금 방지)
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("이미 정산 완료된 내역입니다.");
        }

        User worker = payment.getUser();

        // 3. 입금할 계좌 조회 및 소유주 검증
        Account targetAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌 정보 없음"));

        if (!targetAccount.getUser().getId().equals(worker.getId())) {
            throw new IllegalArgumentException("이 계좌는 해당 알바생의 계좌가 아닙니다.");
        }

        // 4. 입금 실행 및 상태 변경
        long amount = payment.getTotalAmount();
        targetAccount.deposit(amount);

        // 엔티티의 수정된 메서드 호출 (상태를 COMPLETED로 바꾸고 날짜 기록)
        payment.completePayment();

        // 5. 복호화 및 결과 반환
        String decryptedAccount = aesUtil.decrypt(targetAccount.getAccountNumber());
        return String.format("[기존내역 확정] %s님께 %d원 입금 완료! (잔액: %d원)",
                worker.getName(), amount, targetAccount.getBalance());
    }

    // 3. 실시간 예상 급여 조회 (알바생)
    @Transactional(readOnly = true)
    public SalaryDto.EstimatedResponse getEstimatedSalary(Long storeId, Long userId, int year, int month) {
        // 1. 유저 정보 및 시급 조회
        User user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Integer hourlyWage = (user.getHourlyWage() != null && user.getHourlyWage() > 0)
                ? user.getHourlyWage() : 9860;

        // 2. 조회 기간 설정 (해당 월의 1일 ~ 마지막 날)
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // 3. 해당 월의 출퇴근 기록 조회 (이미 구현된 AttendanceRepository 메서드 활용)
        List<Attendance> attendances = attendanceRepository.findAllByUserAndCheckInTimeBetween(
                user, start.atStartOfDay(), end.atTime(23, 59, 59));

        // 4. 총 근무 시간 합산
        // (Attendance 엔티티 내부의 calculateTotalHours()가 휴게시간 차감 및 소수점 처리를 이미 수행함)
        double totalHours = attendances.stream()
                .mapToDouble(Attendance::calculateTotalHours)
                .sum();

        // 5. 최종 급여 계산
        // Math.round를 통해 원 단위 반올림 처리
        long rawAmount = Math.round(totalHours * hourlyWage);
        long tax = Math.round(rawAmount * 0.033); // 사업소득세 3.3%
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

        payment.requestSalary(); // WAITING -> REQUESTED
        // TODO: 사장님에게 "정산 요청" 푸시 알림 발송
    }

    // 5. 월별 급여 목록 조회 (사장님용 - 총 정산액 포함)
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlySalaryList(Long storeId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // 1. 해당 매장/월의 모든 정산 내역 조회
        List<SalaryPayment> payments = salaryPaymentRepository.findAllByStoreAndPeriod(storeId, start, end);

        // 2. 전체 정산 금액 합계 계산
        long totalAmount = payments.stream()
                .mapToLong(SalaryPayment::getTotalAmount)
                .sum();

        // 3. DTO 변환 (알바생별 상세 내역)
        List<SalaryDto.MonthlyResponse> list = payments.stream().map(p -> SalaryDto.MonthlyResponse.builder()
                .name(p.getUser().getName())
                .amount(p.getTotalAmount())
                .status(p.getStatus().toString())
                .build()).collect(Collectors.toList());

        // 4. 결과 반환 (return 문 추가!)
        return Map.of(
                "totalAmount", totalAmount,
                "employeeCount", list.size(),
                "payments", list
        );
    }

    // 1. 급여 내역 조회 (알바생용 리스트)
    @Transactional(readOnly = true)
    public List<SalaryDto.HistoryResponse> getSalaryHistory(Long userId) {
        User user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        List<SalaryPayment> payments = salaryPaymentRepository.findAllByUserOrderByPeriodStartDesc(user);

        return payments.stream().map(p -> SalaryDto.HistoryResponse.builder()
                .id(p.getId())
                .month(p.getPeriodStart().getMonthValue() + "월") // "1월" 형식
                .amount(p.getTotalAmount())
                .status(p.getStatus().toString())
                .build()).collect(Collectors.toList());
    }

    // 2. 명세서 이메일 발송 (비동기)
    @Async // 응답 지연 방지
    public void sendPayslipEmail(Long paymentId) {
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("정산 내역 없음"));

        // TODO: iText 또는 Thymeleaf를 사용하여 PDF 생성 로직 추가
        // JavaMailSender를 이용해 payment.getUser().getEmail()로 전송
        System.out.println("이메일 발송 완료: " + payment.getUser().getEmail());
    }

    // 3. 급여대장 엑셀 다운로드 (Apache POI 활용)
    public void generateSalaryExcel(Long storeId, int year, int month, HttpServletResponse response) throws IOException {
        // 1. 데이터 조회
        List<SalaryPayment> payments = salaryPaymentRepository.findAllByStoreIdAndYearAndMonth(storeId, year, month);
        Store store = storeRepository.findById(storeId).orElseThrow();

        // 2. 엑셀 워크북 및 시트 생성
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(year + "년 " + month + "월 급여대장");

        // 3. 헤더 생성 (첫 번째 행)
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("성명");
        headerRow.createCell(1).setCellValue("지급액");
        headerRow.createCell(2).setCellValue("정산상태");
        headerRow.createCell(3).setCellValue("정산일자");

        // 4. 데이터 쓰기 (두 번째 행부터)
        int rowIdx = 1;
        for (SalaryPayment payment : payments) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(payment.getUser().getName());    // 알바생 이름
            row.createCell(1).setCellValue(payment.getTotalAmount());        // 총 급여액
            row.createCell(2).setCellValue(payment.getStatus().toString()); // 상태 (COMPLETED 등)
            row.createCell(3).setCellValue(payment.getCreatedAt().toString()); // 정산 시점
        }

        // 5. [파일명 설정] 한글 깨짐 방지 인코딩 적용
        String fileName = year + "년" + month + "월_급여대장_" + store.getName();
        String encodedFileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // 파일명 인코딩 표준에 맞춰 설정
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + ".xlsx\"; filename*=UTF-8''" + encodedFileName + ".xlsx");

        // 6. 엑셀 파일 출력 및 닫기
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @Transactional
    public String executeNewPayment(Long storeId, Long userId, Long accountId, int year, int month) {
        // 1. [해결] worker, store, targetAccount가 누구인지 DB에서 찾아오기
        User worker = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("알바생 정보를 찾을 수 없습니다."));

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("매장 정보를 찾을 수 없습니다."));

        Account targetAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌 정보를 찾을 수 없습니다."));

        // 2. 급여 계산 (이미 만들어둔 메서드 활용)
        SalaryDto.EstimatedResponse estimate = getEstimatedSalary(storeId, userId, year, month);

        // 3. 새로운 정산 레코드 생성 (INSERT)
        SalaryPayment newPayment = SalaryPayment.builder()
                .user(worker)      // 이제 worker를 인식합니다!
                .store(store)      // 이제 store를 인식합니다!
                .account(targetAccount)
                .totalAmount(estimate.getAmount())
                .totalHours(estimate.getTotalHours())
                .periodStart(LocalDate.of(year, month, 1))
                .periodEnd(LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth()))
                .status(PaymentStatus.WAITING) // 초기 상태
                .build();

        // 4. 입금 처리 및 확정
        targetAccount.deposit(estimate.getAmount()); // 이제 targetAccount를 인식합니다!
        newPayment.completePayment(); // 상태를 COMPLETED로 변경 및 시간 기록

        salaryPaymentRepository.save(newPayment);

        // 5. 결과 반환
        String displayAccount;
        try {
            displayAccount = aesUtil.decrypt(targetAccount.getAccountNumber());
        } catch (Exception e) {
            // 복호화 실패 시 (암호화 안 된 데이터일 때) 그냥 원본 출력
            displayAccount = targetAccount.getAccountNumber();
        }

        return String.format("[%s] %s님께 %d원 정산 완료! (계좌: %s, 잔액: %d원)",
                store.getName(), worker.getName(), estimate.getAmount(), displayAccount, targetAccount.getBalance());

    }
}