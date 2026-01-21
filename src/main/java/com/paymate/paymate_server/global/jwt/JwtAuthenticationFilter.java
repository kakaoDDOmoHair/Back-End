package com.paymate.paymate_server.global.jwt;

import com.paymate.paymate_server.domain.member.repository.MemberRepository; // 👈 import 추가
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String token = resolveToken((HttpServletRequest) request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getSubject(token);

            // 1. 스프링 컨텍스트에서 'MemberRepository'를 꺼냅니다. (Service 아님!)
            MemberRepository memberRepository = WebApplicationContextUtils
                    .getRequiredWebApplicationContext(request.getServletContext())
                    .getBean(MemberRepository.class);

            // 2. [유령 작전] 여기서 서비스를 'new'로 직접 만듭니다.
            // 스프링은 이 서비스의 존재를 모르므로, 초기화 에러를 낼 수가 없습니다.
            CustomUserDetailsService customUserDetailsService = new CustomUserDetailsService(memberRepository);

            // 3. 이제 사용합니다.
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

            if (userDetails != null) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}