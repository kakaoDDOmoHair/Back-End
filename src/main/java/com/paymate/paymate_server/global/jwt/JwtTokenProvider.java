package com.paymate.paymate_server.global.jwt;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final MemberRepository memberRepository;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey, MemberRepository memberRepository) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.memberRepository = memberRepository;
    }

    // 1. 토큰 생성 (로그인 성공 시 호출)
    public TokenInfo generateToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        // Access Token 생성 (30분 유효)
        Date accessTokenExpiresIn = new Date(now + 1800000); // 30분
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim("auth", authorities)
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token 생성 (1일 유효)
        String refreshToken = Jwts.builder()
                .setExpiration(new Date(now + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return TokenInfo.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 2. 토큰에서 인증 정보(유저 정보) 꺼내기 — CustomUserDetails 사용 (modifications 등 @AuthenticationPrincipal CustomUserDetails 매칭용)
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);
        if (claims.get("auth") == null) {
            return null;
        }

        // subject = 로그인 시 authentication.getName() = user.getUsername() (AuthService.getAuthentication)
        Optional<User> userOpt = memberRepository.findByUsername(claims.getSubject());
        if (userOpt.isEmpty()) {
            log.debug("JWT subject(username)에 해당하는 사용자가 없음: {}", claims.getSubject());
            return null;
        }

        CustomUserDetails principal = new CustomUserDetails(userOpt.get());
        return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
    }

    // 3. 토큰이 유효한지 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
    public String createResetToken(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + 600000); // 10분 (10 * 60 * 1000)

        return Jwts.builder()
                .setSubject(email) // 이메일만 담음
                .claim("type", "RESET") // "이건 리셋용 토큰이다"라고 명시
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    // 4. 토큰에서 회원 정보(이메일) 추출 (비밀번호 재설정 및 필터에서 공통 사용)
    public String getSubject(String token) {
        try {
            // parseClaims를 통해 토큰의 모든 정보를 가져온 후, 그 중 Subject(보통 이메일)를 반환합니다.
            Claims claims = parseClaims(token);

            if (claims == null) {
                throw new IllegalArgumentException("토큰의 클레임 정보를 가져올 수 없습니다.");
            }

            return claims.getSubject();
        } catch (Exception e) {
            // 토큰이 올바르지 않거나 해석 중 에러가 발생할 경우에 대한 처리
            throw new IllegalArgumentException("토큰에서 사용자 정보를 추출하는 데 실패했습니다: " + e.getMessage());
        }
    }
}