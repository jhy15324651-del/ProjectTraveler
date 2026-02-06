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

        registry.addResourceHandler("/uploads/info/**")
                .addResourceLocations("file:///C:/lms-uploads/images/info/");
    }
}
