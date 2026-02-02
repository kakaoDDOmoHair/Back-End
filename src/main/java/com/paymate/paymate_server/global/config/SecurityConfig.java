package com.paymate.paymate_server.global.config;

import com.paymate.paymate_server.global.jwt.JwtAuthenticationFilter;
import com.paymate.paymate_server.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 0. CORS 허용 (프론트/ngrok 등에서 네트워크 오류 방지)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 1. CSRF, FormLogin, HttpBasic 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 2. 세션 미사용 (JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // [공통] Swagger & 기본 인증
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/users/login", "/api/v1/users/join").permitAll()
                        .requestMatchers("/api/v1/users/password", "/api/v1/users/withdraw").permitAll()
                        .requestMatchers("/api/v1/users/me", "/api/v1/users/detail").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // [테스트용 권한 해제] - 나중에 보안 강화 필요
                        .requestMatchers("/api/v1/stores/**").permitAll()
                        .requestMatchers("/api/v1/contracts/**").permitAll()
                        .requestMatchers("/api/v1/manuals/**").permitAll() // 👈 [추가됨] 매뉴얼 기능 허용!
                        .requestMatchers("/api/v1/test/**").permitAll()    // 👈 [추천] 가짜 은행 등 테스트 API 허용

                        .requestMatchers("/api/v1/verification/**").permitAll()
                        .requestMatchers("/api/v1/schedules/**").permitAll()
                        .requestMatchers("/api/v1/attendances/**").permitAll()
                        .requestMatchers("/api/v1/salary/**").permitAll()
                        .requestMatchers("/api/v1/todos/**").permitAll()
                        .requestMatchers("/api/v1/modifications/**").permitAll()
                        .requestMatchers("/api/v1/notifications/**").permitAll()
                        // 업로드된 정적 파일(계약서 이미지 등)은 공개 조회 허용
                        .requestMatchers("/uploads/**").permitAll()
                        .anyRequest().authenticated()
                )

                // 4. JWT 필터 추가
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** CORS: 프론트(Expo, ngrok 등)에서 API 호출 시 차단 방지 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false); // * 허용 시 credentials false 권장

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}