package com.paymate.paymate_server.domain.auth.service;

import com.paymate.paymate_server.domain.auth.dto.*;
import com.paymate.paymate_server.domain.auth.entity.RefreshToken;
import com.paymate.paymate_server.domain.auth.entity.VerificationCode;
import com.paymate.paymate_server.domain.auth.repository.RefreshTokenRepository;
import com.paymate.paymate_server.domain.auth.repository.VerificationCodeRepository;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.global.jwt.JwtTokenProvider;
import com.paymate.paymate_server.global.jwt.TokenInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;
import org.springframework.mail.javamail.JavaMailSender; // 👈 추가
import org.springframework.mail.javamail.MimeMessageHelper; // 👈 추가
import jakarta.mail.MessagingException; // 👈 추가
import jakarta.mail.internet.MimeMessage; // 👈 추가
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JavaMailSender mailSender;

    /**
     * [수정] 로그인 (Email -> Username)
     */
    @Transactional
    public TokenResponseDto login(LoginRequestDto request) {
        // 1. 아이디로 사용자 찾기 (기존: findByEmail)
        User user = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 인증 객체 생성 (이제 주체는 Username)
        Authentication authentication = getAuthentication(user);
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // 4. 리프레시 토큰 저장
        // (RefreshToken 엔티티의 필드명이 email이라도, 실제로는 username(식별자)을 저장합니다)
        refreshTokenRepository.save(RefreshToken.builder()
                .email(user.getUsername()) // [중요] 키값을 아이디로 저장
                .token(tokenInfo.getRefreshToken())
                .build());

        return TokenResponseDto.builder()
                .accessToken(tokenInfo.getAccessToken())
                .refreshToken(tokenInfo.getRefreshToken())
                .role(user.getRole().name())
                .name(user.getName())
                .build();
    }

    /**
     * 토큰 재발급 (Reissue)
     */
    @Transactional
    public TokenResponseDto reissue(TokenRequestDto request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(request.getAccessToken());

        // 저장된 토큰 찾기 (저장할 때 username으로 저장했으므로, 여기서도 name으로 찾음)
        RefreshToken refreshToken = refreshTokenRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("로그아웃 된 사용자입니다."));

        if (!refreshToken.getToken().equals(request.getRefreshToken())) {
            throw new IllegalArgumentException("토큰 정보가 일치하지 않습니다.");
        }

        TokenInfo newTokenInfo = jwtTokenProvider.generateToken(authentication);

        refreshToken.updateToken(newTokenInfo.getRefreshToken());

        return TokenResponseDto.builder()
                .accessToken(newTokenInfo.getAccessToken())
                .refreshToken(newTokenInfo.getRefreshToken())
                .role(authentication.getAuthorities().toString().replaceAll("[\\[\\]]", ""))
                .name(authentication.getName())
                .build();
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(TokenRequestDto request) {
        if (!jwtTokenProvider.validateToken(request.getAccessToken())) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }
        Authentication authentication = jwtTokenProvider.getAuthentication(request.getAccessToken());

        // 아이디(Username) 기반으로 삭제
        refreshTokenRepository.findByEmail(authentication.getName())
                .ifPresent(refreshTokenRepository::delete);
    }

    // =========================================================================
    // ▼ [NEW] ID 찾기, 비번 검증, 계좌 인증
    // =========================================================================

    /**
     * [수정] 현재 비밀번호 검증 (Email -> Username)
     */
    public boolean verifyPassword(String username, PasswordVerifyRequestDto request) {
        User user = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return passwordEncoder.matches(request.getPassword(), user.getPassword());
    }

    /**
     * [유지] 계좌 실명 인증 (Mock)
     */
    public AccountVerifyResponseDto verifyAccount(AccountVerifyRequestDto request) {
        if ("오류".equals(request.getOwnerName())) {
            throw new IllegalArgumentException("계좌 인증에 실패했습니다. (예금주 불일치)");
        }

        return AccountVerifyResponseDto.builder()
                .bankName("신한은행")
                .ownerName(request.getOwnerName())
                .verificationToken(UUID.randomUUID().toString())
                .build();
    }

    @Transactional
    public void sendVerificationCode(String email, String name) {
        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!user.getName().equals(name)) {
            throw new IllegalArgumentException("이름이 일치하지 않습니다.");
        }

        String code = String.valueOf(100000 + new Random().nextInt(900000));

        verificationCodeRepository.save(VerificationCode.builder()
                .email(email)
                .code(code)
                .expiryDate(LocalDateTime.now().plusMinutes(3))
                .build());

        // 👇 [수정] Mock 로그 대신 실제 메일 전송 로직 호출
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("[PayMate] 본인확인 인증번호입니다.");

            // HTML 형식으로 가독성 있게 구성
            String content = "<div style='margin:20px; padding:20px; border:1px solid #ddd;'>" +
                    "<h3>안녕하세요, PayMate입니다.</h3>" +
                    "<p>본인 확인을 위한 인증번호는 다음과 같습니다.</p>" +
                    "<h2 style='color: #4A90E2;'>" + code + "</h2>" +
                    "<p>3분 이내에 입력해 주세요.</p>" +
                    "</div>";

            helper.setText(content, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("메일 발송에 실패했습니다. 관리자에게 문의하세요.");
        }
    }

    @Transactional
    public String verifyCodeAndGetId(String email, String code) {
        // 👈 들어오는 값의 공백을 제거합니다.
        String trimmedEmail = email.trim();
        String trimmedCode = code.trim();

        System.out.println("검증 시도 -> 이메일: [" + trimmedEmail + "], 코드: [" + trimmedCode + "]");

        VerificationCode savedInfo = verificationCodeRepository.findById(trimmedEmail)
                .orElseThrow(() -> new IllegalArgumentException("인증번호가 만료되었거나 요청되지 않았습니다."));

        // 저장된 코드와 비교할 때도 공백 제거
        if (!savedInfo.getCode().trim().equals(trimmedCode)) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        // [수정] 이메일로 유저를 찾아서 -> 진짜 아이디(Username)를 반환해야 함!
        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

        String username = user.getUsername();

        verificationCodeRepository.delete(savedInfo);
        return username; // 마스킹된 아이디 반환
    }

    // =========================================================================

    // [수정] User -> Authentication 변환 (Email -> Username)
    private Authentication getAuthentication(User user) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(user.getRole().name()));
        // Principal을 username으로 설정
        return new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
    }

/**
 * [수정 완료] 비밀번호 재설정용 유저 확인 (공백 제거 로직 추가)
 */
    @Transactional
    public void checkUserForReset(PasswordResetCheckRequestDto request) {
        // 1. 입력값에서 공백을 미리 제거합니다.
        String trimmedEmail = request.getEmail().trim();
        String trimmedName = request.getName().trim();
        String trimmedUsername = request.getUsername().trim();

        // 2. 이메일로 유저를 찾습니다.
        User user = memberRepository.findByEmail(trimmedEmail)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 3. 이름이 일치하는지 확인합니다.
        if (!user.getName().equals(trimmedName)) {
            throw new IllegalArgumentException("사용자 정보가 일치하지 않습니다.");
        }

        // 4. 아이디(Username)가 일치하는지 확인합니다.
        if (!user.getUsername().equals(trimmedUsername)) {
            throw new IllegalArgumentException("아이디 정보가 일치하지 않습니다.");
        }

        // 모든 정보가 일치하면 실제 메일 발송을 호출합니다.
        this.sendVerificationCode(trimmedEmail, trimmedName);
    }
    /**
     * [유지] 코드 검증 및 리셋 토큰 발급
     */
    @Transactional
    public String verifyCodeForReset(PasswordResetVerifyRequestDto request) {
        VerificationCode savedInfo = verificationCodeRepository.findById(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("인증번호가 만료되었거나 존재하지 않습니다."));

        if (savedInfo.isExpired()) {
            verificationCodeRepository.delete(savedInfo);
            throw new IllegalArgumentException("인증번호가 만료되었습니다.");
        }
        if (!savedInfo.getCode().equals(request.getAuthCode())) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        verificationCodeRepository.delete(savedInfo);

        // 리셋 토큰 발급 (이메일 기준)
        return jwtTokenProvider.createResetToken(request.getEmail());
    }

    /**
     * [유지] 비밀번호 변경
     */
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        if (!jwtTokenProvider.validateToken(resetToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(resetToken);
        String email = authentication.getName(); // 리셋 토큰은 이메일로 만들었음

        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updatePassword(passwordEncoder.encode(newPassword));

        // [중요] 비밀번호 변경 시 모든 세션 로그아웃
        // RefreshToken 테이블에서 이 유저(Username 키)의 토큰을 지워야 함
        refreshTokenRepository.findByEmail(user.getUsername()) // 키값은 Username임
                .ifPresent(refreshTokenRepository::delete);
    }
}