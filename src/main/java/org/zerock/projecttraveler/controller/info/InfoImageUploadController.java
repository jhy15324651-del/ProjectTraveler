package org.zerock.projecttraveler.controller.info;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/info")
@RequiredArgsConstructor
public class InfoImageUploadController {

    // ✅ 원하는 저장 경로로 바꿔도 됨 (reviews와 같은 스타일 추천)
    private static final String UPLOAD_DIR = "C:/lms-uploads/images/info";

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile image) throws IOException {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "empty file"));
        }

        String original = StringUtils.cleanPath(image.getOriginalFilename() == null ? "image" : image.getOriginalFilename());
        String ext = "";

        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot);

        String filename = UUID.randomUUID() + ext;

        Path dir = Paths.get(UPLOAD_DIR);
        Files.createDirectories(dir);

        Path target = dir.resolve(filename);
        Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // ✅ WebConfig에서 /uploads/info/** -> UPLOAD_DIR 매핑하면 여기 URL이 바로 접근됨
        String url = "/uploads/info/" + filename;

        return ResponseEntity.ok(Map.of("url", url));
    }
}
