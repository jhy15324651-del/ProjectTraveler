package org.zerock.projecttraveler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 설정 클래스
 *
 * Unity WebGL에서 Spring Boot API 호출 시 CORS 에러 해결을 위한 설정.
 * - 개발환경: LAN 전체 허용 (localhost, 127.0.0.1, 192.168.*.*)
 * - 운영환경: 아래 allowedOriginPatterns를 실제 도메인으로 제한할 것
 *
 * @see SecurityConfig#filterChain - .cors() 활성화 필요
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ============================================================
        // [개발용] 모든 Origin 허용 - LAN 환경에서 팀원 테스트용
        // ============================================================
        // 패턴으로 허용: localhost, 127.0.0.1, 192.168.*.* 등
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://192.168.*.*:*",    // LAN 대역
            "http://10.*.*.*:*",        // 사설 IP 대역
            "http://172.16.*.*:*",      // 사설 IP 대역
            "http://172.17.*.*:*",
            "http://172.18.*.*:*",
            "http://172.19.*.*:*",
            "http://172.20.*.*:*",
            "http://172.21.*.*:*",
            "http://172.22.*.*:*",
            "http://172.23.*.*:*",
            "http://172.24.*.*:*",
            "http://172.25.*.*:*",
            "http://172.26.*.*:*",
            "http://172.27.*.*:*",
            "http://172.28.*.*:*",
            "http://172.29.*.*:*",
            "http://172.30.*.*:*",
            "http://172.31.*.*:*"
        ));

        // ============================================================
        // [운영용] 실제 도메인으로 제한 시 위 코드를 주석 처리하고 아래 사용
        // ============================================================
        // configuration.setAllowedOrigins(List.of(
        //     "https://your-production-domain.com",
        //     "https://www.your-production-domain.com"
        // ));

        // 허용 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 허용 헤더 (모든 헤더 허용)
        configuration.setAllowedHeaders(List.of("*"));

        // 노출 헤더 (클라이언트에서 접근 가능한 응답 헤더)
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-CSRF-TOKEN"
        ));

        // 쿠키/인증정보 포함 허용 (세션 기반 인증에 필요)
        configuration.setAllowCredentials(true);

        // Preflight 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        // 모든 경로에 CORS 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
