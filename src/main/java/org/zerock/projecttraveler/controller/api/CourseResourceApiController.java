package org.zerock.projecttraveler.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.entity.CourseResource;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.CourseResourceService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/course-resources")
@RequiredArgsConstructor
@Slf4j
public class CourseResourceApiController {

    private final CourseResourceService resourceService;

    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Object>> getResources(@PathVariable Long courseId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        if (!SecurityUtils.isAdmin() && !resourceService.hasAccess(userId, courseId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("수강 권한이 없습니다."));
        }

        List<CourseResource> resources = resourceService.getActiveResources(courseId);

        // 유닛별 그룹핑하여 반환
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, List<CourseResource>> grouped = resourceService.groupByUnit(resources);

        for (Map.Entry<String, List<CourseResource>> entry : grouped.entrySet()) {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("unitName", entry.getKey());
            group.put("resources", entry.getValue().stream().map(r -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", r.getId());
                item.put("title", r.getTitle());
                item.put("description", r.getDescription());
                item.put("originalFileName", r.getOriginalFileName());
                item.put("fileSize", r.getFileSize());
                item.put("fileType", r.getFileType());
                item.put("downloadCount", r.getDownloadCount());
                item.put("createdAt", r.getCreatedAt().toString());
                return item;
            }).collect(Collectors.toList()));
            result.add(group);
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/download/{resourceId}")
    public ResponseEntity<?> downloadResource(@PathVariable Long resourceId) {
        try {
            Long userId = SecurityUtils.getCurrentUserIdOrThrow();
            CourseResource resource = resourceService.downloadResource(resourceId);

            // 접근 권한 확인
            if (!SecurityUtils.isAdmin() && !resourceService.hasAccess(userId, resource.getCourse().getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("수강 권한이 없습니다."));
            }

            Path filePath = Paths.get(resourceService.getStoragePath(), resource.getStoredFileName());
            Resource fileResource = new UrlResource(filePath.toUri());

            if (!fileResource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String encodedFileName = URLEncoder.encode(resource.getOriginalFileName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(fileResource);
        } catch (Exception e) {
            log.error("파일 다운로드 오류", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("파일 다운로드에 실패했습니다."));
        }
    }
}
