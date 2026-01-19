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
    private final VerificationCodeRepository verificationCodeRepository; // [NEW] 추가됨
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 로그인
     */
    @Transactional
    public TokenResponseDto login(LoginRequestDto request) {
        User user = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        Authentication authentication = getAuthentication(user);
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        refreshTokenRepository.save(RefreshToken.builder()
                .email(user.getEmail())
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
                // Role 정보가 문자열로 박히는 것 방지 (Authentication에서 파싱 필요 시 수정 가능)
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
    // ▼ [NEW] 새로 추가된 기능들 (ID 찾기, 비번 검증, 계좌 인증)
    // =========================================================================

    /**
     * [NEW] 현재 비밀번호 검증 (회원탈퇴 전 본인 확인용)
     */
    public boolean verifyPassword(String email, PasswordVerifyRequestDto request) {
        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return passwordEncoder.matches(request.getPassword(), user.getPassword());
    }

    /**
     * [NEW] 계좌 실명 인증 (Mock - 가짜 구현)
     */
    public AccountVerifyResponseDto verifyAccount(AccountVerifyRequestDto request) {
        // 실제로는 오픈뱅킹 API를 호출해야 함. 여기서는 테스트용 로직 적용.
        // 예금주 이름이 "오류"인 경우만 실패 처리
        if ("오류".equals(request.getOwnerName())) {
            throw new IllegalArgumentException("계좌 인증에 실패했습니다. (예금주 불일치)");
        }

        return AccountVerifyResponseDto.builder()
                .bankName("신한은행") // 가짜 은행명 고정
                .ownerName(request.getOwnerName())
                .verificationToken(UUID.randomUUID().toString()) // 임의의 인증 토큰 생성
                .build();
    }

    /**
     * [NEW] ID 찾기 - 인증번호 발송 (콘솔 출력 Mock)
     */
    @Transactional
    public void sendVerificationCode(String email, String name) {
        // 1. 사용자 확인
        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!user.getName().equals(name)) {
            throw new IllegalArgumentException("이름이 일치하지 않습니다.");
        }

        // 2. 인증번호 생성 (6자리 난수)
        String code = String.valueOf(100000 + new Random().nextInt(900000));

        // 3. DB에 저장 (3분 유효)
        verificationCodeRepository.save(VerificationCode.builder()
                .email(email)
                .code(code)
                .expiryDate(LocalDateTime.now().plusMinutes(3)) // 3분 뒤 만료
                .build());

        // 4. 메일 발송 대신 콘솔 출력
        System.out.println("=========================================");
        System.out.println("[PayMate 이메일 발송 Mock]");
        System.out.println("수신자: " + email);
        System.out.println("인증번호: " + code);
        System.out.println("=========================================");
    }

    /**
     * [NEW] ID 찾기 - 인증번호 검증 및 마스킹된 ID 반환
     */
    @Transactional
    public String verifyCodeAndGetId(String email, String code) {
        // 1. 코드 조회
        VerificationCode savedInfo = verificationCodeRepository.findById(email)
                .orElseThrow(() -> new IllegalArgumentException("인증번호가 만료되었거나 요청되지 않았습니다."));

        // 2. 만료 및 일치 여부 확인
        if (savedInfo.isExpired()) {
            verificationCodeRepository.delete(savedInfo); // 만료된 데이터 삭제
            throw new IllegalArgumentException("인증번호가 만료되었습니다.");
        }

        if (!savedInfo.getCode().equals(code)) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        // 3. 인증 성공 시, 마스킹된 ID 생성 (예: tes***@gmail.com)
        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (prefix.length() > 3) {
            prefix = prefix.substring(0, 3) + "***";
        } else {
            prefix = prefix + "***";
        }

        // 4. 사용된 코드 삭제
        verificationCodeRepository.delete(savedInfo);

        return prefix + domain;
    }

    // =========================================================================

    // (편의 메서드) User -> Authentication 변환
    private Authentication getAuthentication(User user) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(user.getRole().name()));
        return new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
    }
    @Transactional
    public void checkUserForReset(PasswordResetCheckRequestDto request) {
        // 이름, 이메일 일치 확인
        User user = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!user.getName().equals(request.getName())) {
            throw new IllegalArgumentException("사용자 정보가 일치하지 않습니다.");
        }

        // 인증번호 생성 및 저장 (기존 메서드 재활용!)
        // 기존 sendVerificationCode는 void라 내부 로직을 가져오거나,
        // 여기서 바로 저장 로직을 써도 됩니다. 깔끔하게 기존 로직을 호출하는 방식은 아래와 같습니다.

        // (단, 기존 sendVerificationCode가 public이면 바로 호출 가능)
        this.sendVerificationCode(request.getEmail(), request.getName());
    }

    /**
     * 2. 코드 검증 후 '리셋 토큰' 발급 (POST /verify-code)
     */
    @Transactional
    public String verifyCodeForReset(PasswordResetVerifyRequestDto request) {
        // 기존 인증번호 검증 로직 활용 (마스킹 ID 반환하는 메서드 대신, 검증만 하는 로직이 필요하지만)
        // 여기서는 코드를 직접 조회해서 검증하겠습니다.

        VerificationCode savedInfo = verificationCodeRepository.findById(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("인증번호가 만료되었거나 존재하지 않습니다."));

        if (savedInfo.isExpired()) {
            verificationCodeRepository.delete(savedInfo);
            throw new IllegalArgumentException("인증번호가 만료되었습니다.");
        }
        if (!savedInfo.getCode().equals(request.getAuthCode())) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        // 인증 성공! 사용된 코드 삭제
        verificationCodeRepository.delete(savedInfo);

        // ★중요★ 비밀번호 변경 권한이 담긴 'Reset Token' 발급
        return jwtTokenProvider.createResetToken(request.getEmail());
    }

    /**
     * 3. 진짜 비밀번호 변경 (PATCH /password)
     */
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        // 1. 리셋 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(resetToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
        }

        // 2. 토큰에서 이메일 추출
        Authentication authentication = jwtTokenProvider.getAuthentication(resetToken);
        String email = authentication.getName();

        // 3. 유저 조회
        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 4. 비밀번호 암호화 및 변경
        user.updatePassword(passwordEncoder.encode(newPassword));

        // 5. [중요] 기존 세션 만료 처리 (모든 기기에서 로그아웃)
        // 비밀번호가 바뀌었으니, 기존에 발급된 Refresh Token을 DB에서 삭제합니다.
        refreshTokenRepository.findByEmail(email)
                .ifPresent(refreshTokenRepository::delete);
    }

}