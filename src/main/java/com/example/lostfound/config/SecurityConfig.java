package com.example.lostfound.config;

import com.example.lostfound.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 인증, 인가, 로그인 후 이동 규칙을 설정한다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    /**
     * 애플리케이션 보안 필터 체인을 구성한다.
     *
     * @param http 보안 설정 빌더
     * @return 구성된 필터 체인
     * @throws Exception 보안 설정 생성 실패 시 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/signup", "/error", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.POST, "/items/*/comments", "/items/*/comments/*/delete").authenticated()
                .requestMatchers("/items/**", "/uploads/**", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(this::handleLoginSuccess)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .userDetailsService(customUserDetailsService);

        return http.build();
    }

    /**
     * 로그인 성공 후 저장된 요청, 명시적 redirect 파라미터, 권한 순서로 이동 경로를 결정한다.
     *
     * @param request 현재 요청
     * @param response 현재 응답
     * @param authentication 인증 정보
     * @throws IOException 리다이렉트 실패 시 예외
     * @throws ServletException 저장 요청 처리 실패 시 예외
     */
    private void handleLoginSuccess(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Authentication authentication) throws IOException, ServletException {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        var savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            // 보호된 페이지에서 로그인한 경우 원래 요청하던 페이지로 돌려보낸다.
            SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
            handler.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        String redirect = request.getParameter("redirect");
        if (StringUtils.hasText(redirect) && redirect.startsWith("/") && !redirect.startsWith("//")) {
            response.sendRedirect(redirect);
            return;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        response.sendRedirect(isAdmin ? "/admin/pending" : "/");
    }

    /**
     * 비밀번호 암호화에 사용할 인코더를 생성한다.
     *
     * @return 비밀번호 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
