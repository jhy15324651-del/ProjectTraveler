package org.zerock.projecttraveler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 🎥 기존 영상 업로드 경로
    @Value("${app.upload.video-path:C:/lms-uploads/videos}")
    private String videoUploadPath;

    // 🖼️ 새로 추가할 이미지 업로드 경로
    @Value("${app.upload.image-path:C:/lms-uploads/images}")
    private String imageUploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // ✅ 기존 영상 파일 서빙 (그대로 유지)
        registry.addResourceHandler("/uploads/videos/**")
                .addResourceLocations("file:" + videoUploadPath + "/");

        // ✅ 여행 후기 이미지 파일 서빙 (추가)
        registry.addResourceHandler("/uploads/reviews/**")
                .addResourceLocations("file:" + imageUploadPath + "/reviews/");

        // ✅ 플래너 썸네일 이미지 파일 서빙
        registry.addResourceHandler("/uploads/planners/**")
                .addResourceLocations("file:" + imageUploadPath + "/planners/");

        // ✅ 일정 이미지 파일 서빙
        registry.addResourceHandler("/uploads/itineraries/**")
                .addResourceLocations("file:" + imageUploadPath + "/itineraries/");

        // ⭐ info 썸네일 (🔥 추가 핵심)
        registry.addResourceHandler("/uploads/info-thumbnail/**")
                .addResourceLocations("file:" + ensureSlash(imageUploadPath) + "info-thumbnail/");

        // 본문 이미지
        registry.addResourceHandler("/uploads/info-content/**")
                .addResourceLocations("file:" + imageUploadPath + "/info-content/");

        // ✅ 강좌 자료실 파일 서빙
        registry.addResourceHandler("/uploads/course-resources/**")
                .addResourceLocations("file:" + imageUploadPath + "/course-resources/");

        // ✅ Q&A 이미지 파일 서빙
        registry.addResourceHandler("/uploads/qna/**")
                .addResourceLocations("file:" + imageUploadPath + "/qna/");

        // ✅html 폴더 내부의 모든 자원을 매핑
        registry.addResourceHandler("/html/**")
                .addResourceLocations("classpath:/static/html/");

        // =====================================================
        // ✅ [추가] 기본 static 리소스 명시 매핑 (🔥 핵심)
        // - /css/style.css, /js/login.js 등이 404 나는 문제 해결
        // =====================================================
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        registry.addResourceHandler("/unity/**")
                .addResourceLocations("classpath:/static/unity/");
    }

    private String ensureSlash(String path) {
        if (!path.endsWith("/") && !path.endsWith("\\")) {
            return path + "/";
        }
        return path;
    }
}
