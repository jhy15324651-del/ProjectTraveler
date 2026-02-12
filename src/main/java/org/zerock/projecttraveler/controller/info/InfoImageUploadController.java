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

    // application-local.yml:
    // app.upload.image-path: C:/lms-uploads/images
    @Value("${app.upload.image-path:C:/lms-uploads/images}")
    private String imageUploadPath;

    // 저장 폴더명 (원하는 폴더명 고정)
    private static final String SUB_DIR = "info-thumbnail";

    // 허용 확장자 (안전장치)
    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile image) throws IOException {

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

        Path dir = Paths.get(imageUploadPath, SUB_DIR);
        Files.createDirectories(dir);

        Path target = dir.resolve(filename);
        Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // ✅ WebConfig: /uploads/info-thumbnail/** -> file:{imageUploadPath}/info-thumbnail/
        String url = "/uploads/info-thumbnail/" + filename;

        return ResponseEntity.ok(Map.of("url", url));
    }

    private String extractExtLower(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "png"; // 확장자 없으면 png로 처리(원하면 거부로 바꿔도 됨)
        return filename.substring(dot + 1).toLowerCase();
    }
}
