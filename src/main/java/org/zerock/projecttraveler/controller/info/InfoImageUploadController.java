package org.zerock.projecttraveler.controller.info;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/info")
@RequiredArgsConstructor
public class InfoImageUploadController {

    @Value("${app.upload.image-path:C:/lms-uploads/images}")
    private String imageUploadPath;

    // ✅ 폴더 분리
    private static final String THUMB_DIR = "info-thumbnail";
    private static final String CONTENT_DIR = "info-content";

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    // ============================
    // 1) 썸네일 업로드
    // POST /api/admin/info/upload-thumbnail
    // ============================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/upload-thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadThumbnail(@RequestParam("image") MultipartFile image) throws IOException {
        return saveAndReturnUrl(image, THUMB_DIR, "/uploads/info-thumbnail/");
    }

    // ============================
    // 2) 본문(Quill) 이미지 업로드
    // POST /api/admin/info/upload-content-image
    // ============================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/upload-content-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadContentImage(@RequestParam("image") MultipartFile image) throws IOException {
        return saveAndReturnUrl(image, CONTENT_DIR, "/uploads/info-content/");
    }

    // ============================
    // 공통 저장 로직
    // ============================
    private ResponseEntity<Map<String, String>> saveAndReturnUrl(
            MultipartFile image,
            String subDir,
            String urlPrefix
    ) throws IOException {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "empty file"));
        }

        String original = StringUtils.cleanPath(
                image.getOriginalFilename() == null ? "image" : image.getOriginalFilename()
        );

        String ext = extractExtLower(original);
        if (!ALLOWED_EXT.contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("error", "not allowed extension: " + ext));
        }

        String filename = UUID.randomUUID() + "." + ext;

        Path dir = Paths.get(imageUploadPath, subDir);
        Files.createDirectories(dir);

        Path target = dir.resolve(filename);
        Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String url = urlPrefix + filename;
        return ResponseEntity.ok(Map.of("url", url));
    }

    private String extractExtLower(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "png";
        return filename.substring(dot + 1).toLowerCase();
    }
}
