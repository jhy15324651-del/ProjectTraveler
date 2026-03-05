package org.zerock.projecttraveler.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import org.zerock.projecttraveler.security.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CORS 설정 (Unity WebGL CORS 에러 해결)
                // 설정 상세: CorsConfig.java 참조
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // ✅ [추가] iframe/embed 허용 설정 (X-Frame-Options DENY 해결)
                .headers(headers -> headers
                        // iframe 허용
                        .frameOptions(frame -> frame.disable())

                        // Content Security Policy
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "frame-ancestors 'self' " +
                                                "https://192.168.0.32:8443 " +
                                                "https://localhost:8443 " +
                                                "https://localhost:8080 " +
                                                "https://127.0.0.1:8443"
                                )
                        )
                )

            // CSRF 설정
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/admin/courses/api/**", "/admin/enrollments/api/**",
                    "/admin/quiz/api/**", "/admin/course-resources/api/**", "/admin/course-qna/api/**", "/api/user/**", "/api/auth/**") // API는 CSRF 비활성화
            )
            // 인증/인가 규칙
            .authorizeHttpRequests(auth -> auth
                // 정적 리소스 허용
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/html/**").permitAll()
                // Unity WebGL 정적 파일 허용
                .requestMatchers("/unity/**").permitAll()
                // 로그인/회원가입 페이지 허용
                .requestMatchers("/", "/index", "/login", "/register", "/api/auth/**").permitAll()
                // 에러 페이지 허용 (인증 실패 시에도 에러 표시)
                .requestMatchers("/error").permitAll()
                // 후기 페이지 (Unity iframe에서 접근) - 조회는 공개, 작성은 인증 필요
                    .requestMatchers("/reviews", "/reviews/*").permitAll()
                // 관리자 전용
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 학습 API는 로그인 필요
                .requestMatchers("/api/learning/**").authenticated()
                .requestMatchers("/api/enrollments/**").authenticated()
                .requestMatchers("/api/attendance/**").authenticated()
                .requestMatchers("/api/dashboard/**").authenticated()
                // 플래너 API - 공개 플래너 목록은 허용, 나머지는 인증 필요
                .requestMatchers("/api/planner/public").permitAll()
                .requestMatchers("/api/planner/**").authenticated()
                // 수료증 페이지 (검증은 공개, 보기는 인증 필요)
                .requestMatchers("/certificate/verify/**").permitAll()
                .requestMatchers("/certificate/**").authenticated()
                // 나머지 페이지는 로그인 필요
                .requestMatchers("/main", "/learning", "/course-detail", "/mypage",
                               "/guide", "/select", "/quiz", "/planner", "/planner/**").authenticated()
                .anyRequest().authenticated()

            )
            // 로그인 설정
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/select", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            // 로그아웃 설정
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            // Remember Me 설정
            .rememberMe(remember -> remember
                .key("lms-remember-me-key")
                .tokenValiditySeconds(604800) // 7일
                .userDetailsService(userDetailsService)
            )
            // 세션 관리
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
            );

        return http.build();
    }
}
