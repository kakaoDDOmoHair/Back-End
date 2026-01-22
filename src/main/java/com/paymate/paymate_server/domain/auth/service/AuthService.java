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

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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

    /**
     * [유지] ID 찾기 - 인증번호 발송 (이메일로 찾는 것이므로 Email 유지)
     */
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

        System.out.println("=========================================");
        System.out.println("[PayMate 이메일 발송 Mock]");
        System.out.println("수신자: " + email);
        System.out.println("인증번호: " + code);
        System.out.println("=========================================");
    }

    /**
     * [유지] ID 찾기 - 인증번호 검증 및 ID 반환
     */
    @Transactional
    public String verifyCodeAndGetId(String email, String code) {
        VerificationCode savedInfo = verificationCodeRepository.findById(email)
                .orElseThrow(() -> new IllegalArgumentException("인증번호가 만료되었거나 요청되지 않았습니다."));

        if (savedInfo.isExpired()) {
            verificationCodeRepository.delete(savedInfo);
            throw new IllegalArgumentException("인증번호가 만료되었습니다.");
        }

        if (!savedInfo.getCode().equals(code)) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        // [수정] 이메일로 유저를 찾아서 -> 진짜 아이디(Username)를 반환해야 함!
        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

        String username = user.getUsername();

        // 아이디 마스킹 처리 (예: goji***)
        if (username.length() > 3) {
            username = username.substring(0, 3) + "***";
        } else {
            username = username + "***";
        }

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
     * [유지] 비밀번호 재설정용 유저 확인 (이메일 기반)
     */
    @Transactional
    public void checkUserForReset(PasswordResetCheckRequestDto request) {
        User user = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!user.getName().equals(request.getName())) {
            throw new IllegalArgumentException("사용자 정보가 일치하지 않습니다.");
        }

        // 아이디 일치 여부도 체크하고 싶다면 DTO에 아이디 추가 후 여기서 비교 가능
        // if (!user.getUsername().equals(request.getUsername())) ...

        this.sendVerificationCode(request.getEmail(), request.getName());
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