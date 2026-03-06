package org.zerock.projecttraveler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 허용할 Origin (사용자 지정 IP 및 로컬 환경)
        // setAllowedOriginPatterns를 사용하여 포트 번호와 와일드카드를 유연하게 대응합니다.
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://192.168.0.32:8443",
                "https://192.168.0.9:8443",
                "http://192.168.0.32:8080",
                "https://localhost:8443",
                "http://localhost:8080",
                "http://127.0.0.1:*"
        ));

        // 2. 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 3. 허용할 헤더 (Vivox 및 인증 통신에 필요한 핵심 헤더들)
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Cache-Control",
                "Content-Type",
                "X-Requested-With",
                "X-CSRF-TOKEN"
        ));

        // 4. 노출 헤더 (클라이언트 JS/유니티에서 응답 헤더의 Authorization 등을 읽을 수 있게 함)
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-CSRF-TOKEN"));

        // 5. 쿠키 및 인증 정보 포함 허용 (세션 유지에 필수)
        configuration.setAllowCredentials(true);

        // 6. Preflight 캐시 시간 (1시간 동안 브라우저가 매번 OPTIONS 요청을 보내지 않게 함)
        configuration.setMaxAge(3600L);

        // 모든 경로(/**)에 대해 위 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}