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
import org.zerock.projecttraveler.security.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http

                // ✅ [추가] iframe/embed 허용 설정 (X-Frame-Options DENY 해결)
                .headers(headers -> headers
                        // X-Frame-Options 제거(또는 sameOrigin)
                        .frameOptions(frame -> frame.disable())

                        // ✅ 권장: CSP로 허용할 출처만 지정 (유니티 WebGL 주소를 여기에)
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("frame-ancestors 'self' http://127.0.0.1:8080;")
                        )
                )
            // CSRF 설정
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/admin/courses/api/**", "/admin/enrollments/api/**") // API는 CSRF 비활성화
            )
            // 인증/인가 규칙
            .authorizeHttpRequests(auth -> auth
                // 정적 리소스 허용
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                // 로그인/회원가입 페이지 허용
                .requestMatchers("/", "/index", "/login", "/register", "/api/auth/**").permitAll()
                // 관리자 전용
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 학습 API는 로그인 필요
                .requestMatchers("/api/learning/**").authenticated()
                .requestMatchers("/api/enrollments/**").authenticated()
                .requestMatchers("/api/attendance/**").authenticated()
                .requestMatchers("/api/dashboard/**").authenticated()
                // 나머지 페이지는 로그인 필요
                .requestMatchers("/main", "/learning", "/course-detail", "/my-classroom",
                               "/attendance", "/online-learning", "/guide", "/select").authenticated()
                .anyRequest().authenticated()

            )
            // 로그인 설정
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/main", true)
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
