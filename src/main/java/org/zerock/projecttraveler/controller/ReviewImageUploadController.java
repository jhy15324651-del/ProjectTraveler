package org.zerock.projecttraveler.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
public class ReviewImageUploadController {

    // ✅ application.yml: app.upload.image-path (예: C:/lms-uploads/images)
    @Value("${app.upload.image-path:C:/lms-uploads/images}")
    private String imageUploadPath;

    // ✅ 허용 확장자
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    // (선택) 용량 제한: 5MB
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile image) throws IOException {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다."));
        }

        if (image.getSize() > MAX_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("error", "파일 용량이 너무 큽니다. (최대 5MB)"));
        }

        String originalName = StringUtils.cleanPath(image.getOriginalFilename() == null ? "" : image.getOriginalFilename());
        String ext = getExt(originalName).toLowerCase();

        if (ext.isEmpty() || !ALLOWED_EXT.contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("error", "허용되지 않은 확장자입니다. (jpg, jpeg, png, webp, gif)"));
        }

        // ✅ 실제 저장 디렉터리: C:/lms-uploads/images/reviews
        Path uploadDir = Paths.get(imageUploadPath, "reviews").toAbsolutePath().normalize();

        // 폴더 생성
        Files.createDirectories(uploadDir);

        // 파일명 충돌 방지
        String savedName = UUID.randomUUID() + "." + ext;

        // 저장 경로
        Path target = uploadDir.resolve(savedName).normalize();

        // ✅ 저장 (덮어쓰기)
        Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // ✅ WebConfig 매핑과 일치하는 URL 반환
        // WebConfig: /uploads/reviews/** -> file:imageUploadPath + "/"
        // 우리가 실제 저장을 images/reviews 아래에 했으니 URL도 /uploads/reviews/xxx 로 맞추는 게 정답
        String url = "/uploads/reviews/" + savedName;

        return ResponseEntity.ok(Map.of(
                "url", url,
                "fileName", savedName
        ));
    }

    private String getExt(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0) return "";
        return filename.substring(idx + 1);
    }
}
