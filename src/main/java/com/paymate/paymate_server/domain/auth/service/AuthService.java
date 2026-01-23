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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

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
     * 로그인
     */
    @Transactional
    public TokenResponseDto login(LoginRequestDto request) {
        User user = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        Authentication authentication = getAuthentication(user);
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        refreshTokenRepository.save(RefreshToken.builder()
                .email(user.getUsername())
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
     * 토큰 재발급
     */
    @Transactional
    public TokenResponseDto reissue(TokenRequestDto request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(request.getAccessToken());

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

        refreshTokenRepository.findByEmail(authentication.getName())
                .ifPresent(refreshTokenRepository::delete);
    }

    // =========================================================================
    // ▼ ID 찾기, 비번 검증, 계좌 인증, 메일 발송
    // =========================================================================

    public boolean verifyPassword(String username, PasswordVerifyRequestDto request) {
        User user = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return passwordEncoder.matches(request.getPassword(), user.getPassword());
    }

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

    // ⭐ [중요 수정] 인증코드 발송 (Duplicate Key 에러 해결)
    @Transactional
    public void sendVerificationCode(String email, String name) {
        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!user.getName().equals(name)) {
            throw new IllegalArgumentException("이름이 일치하지 않습니다.");
        }

        String code = String.valueOf(100000 + new Random().nextInt(900000));

        // 1. 기존에 발급된 코드가 있는지 확인 (없으면 빈 객체 생성)
        VerificationCode verificationCode = verificationCodeRepository.findById(email)
                .orElse(VerificationCode.builder().email(email).build());

        // 2. 내용 업데이트 (덮어쓰기)
        // ⚠️ 주의: VerificationCode 엔티티에 @Setter가 있어야 합니다!
        verificationCode.setCode(code);
        verificationCode.setExpiryDate(LocalDateTime.now().plusMinutes(3));

        // 3. 저장 (JPA가 알아서 Update 또는 Insert 처리)
        verificationCodeRepository.save(verificationCode);

        // 메일 발송 로직
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("[PayMate] 본인확인 인증번호입니다.");

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
        String trimmedEmail = email.trim();
        String trimmedCode = code.trim();

        VerificationCode savedInfo = verificationCodeRepository.findById(trimmedEmail)
                .orElseThrow(() -> new IllegalArgumentException("인증번호가 만료되었거나 요청되지 않았습니다."));

        if (!savedInfo.getCode().trim().equals(trimmedCode)) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

        String username = user.getUsername();

        verificationCodeRepository.delete(savedInfo);
        return username;
    }

    private Authentication getAuthentication(User user) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(user.getRole().name()));
        return new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
    }

    @Transactional
    public void checkUserForReset(PasswordResetCheckRequestDto request) {
        String trimmedEmail = request.getEmail().trim();
        String trimmedName = request.getName().trim();
        String trimmedUsername = request.getUsername().trim();

        User user = memberRepository.findByEmail(trimmedEmail)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!user.getName().equals(trimmedName)) {
            throw new IllegalArgumentException("사용자 정보가 일치하지 않습니다.");
        }

        if (!user.getUsername().equals(trimmedUsername)) {
            throw new IllegalArgumentException("아이디 정보가 일치하지 않습니다.");
        }

        this.sendVerificationCode(trimmedEmail, trimmedName);
    }


    @Transactional
    public String verifyCodeForReset(PasswordResetVerifyRequestDto request) {
        VerificationCode savedInfo = verificationCodeRepository.findById(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("인증번호가 만료되었거나 존재하지 않습니다."));

        if (savedInfo.isExpired()) {
            verificationCodeRepository.delete(savedInfo);
            throw new IllegalArgumentException("인증번호가 만료되었습니다.");
        }

        // 👇 [수정] 입력받은 코드의 앞뒤 공백 제거 (.trim())
        String inputCode = request.getAuthCode().trim();
        String savedCode = savedInfo.getCode().trim();

        if (!savedCode.equals(inputCode)) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다."); // 👈 여기서 에러 난 것임
        }

        verificationCodeRepository.delete(savedInfo);

        // 리셋 토큰 발급 (이메일 기준)
        return jwtTokenProvider.createResetToken(request.getEmail());
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        // 1. 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(resetToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
        }

        // 2. 수정 지점: Authentication 객체를 통째로 가져오지 말고, 이메일(Subject)만 직접 추출하세요.
        // jwtTokenProvider에 getEmailFromToken 또는 getSubject 같은 메서드가 있을 겁니다.
        String email = jwtTokenProvider.getSubject(resetToken);

        // 3. 사용자 조회
        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 4. 비밀번호 업데이트
        user.updatePassword(passwordEncoder.encode(newPassword));

        // 5. 리프레시 토큰 삭제 (로그아웃 처리)
        refreshTokenRepository.findByEmail(user.getEmail()) // user.getUsername() 대신 email 확인
                .ifPresent(refreshTokenRepository::delete);
    }
}