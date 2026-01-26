package org.zerock.projecttraveler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.video-path:C:/lms-uploads/videos}")
    private String videoUploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드된 비디오 파일 서빙
        registry.addResourceHandler("/uploads/videos/**")
                .addResourceLocations("file:" + videoUploadPath + "/");
    }
}
