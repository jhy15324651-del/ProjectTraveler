package org.zerock.projecttraveler.controller.admin;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.entity.Course;
import org.zerock.projecttraveler.entity.CourseResource;
import org.zerock.projecttraveler.entity.CourseUnit;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.CourseResourceService;
import org.zerock.projecttraveler.service.CourseService;

import java.util.List;

@Controller
@RequestMapping("/admin/course-resources")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminCourseResourceController {

    private final CourseResourceService resourceService;
    private final CourseService courseService;

    @GetMapping("/{courseId}")
    public String resourceListPage(@PathVariable Long courseId, Model model) {
        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));
        List<CourseResource> resources = resourceService.getAllResources(courseId);
        List<CourseUnit> units = courseService.findUnitsWithLessons(courseId);

        model.addAttribute("course", course);
        model.addAttribute("resources", resources);
        model.addAttribute("units", units);
        model.addAttribute("activePage", "admin-courses");
        model.addAttribute("username", SecurityUtils.getCurrentUserDetails()
                .map(u -> u.getFullName()).orElse("관리자"));
        model.addAttribute("isAdmin", true);

        return "admin/course-resource-list";
    }

    @PostMapping("/api/{courseId}/upload")
    @ResponseBody
    public ResponseEntity<ApiResponse<CourseResource>> uploadResource(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "unitId", required = false) Long unitId,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "0") Integer sortOrder) {
        try {
            CourseResource resource = resourceService.uploadResource(courseId, unitId, title, description, file, sortOrder);
            return ResponseEntity.ok(ApiResponse.success("자료가 업로드되었습니다.", resource));
        } catch (Exception e) {
            log.error("자료 업로드 오류", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("업로드에 실패했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<CourseResource>> updateResource(
            @PathVariable Long id,
            @RequestBody ResourceUpdateRequest request) {
        try {
            CourseResource resource = resourceService.updateResource(
                    id, request.getTitle(), request.getDescription(),
                    request.getUnitId(), request.getSortOrder());
            return ResponseEntity.ok(ApiResponse.success("자료가 수정되었습니다.", resource));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable Long id) {
        try {
            resourceService.deleteResource(id);
            return ResponseEntity.ok(ApiResponse.success("자료가 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Data
    public static class ResourceUpdateRequest {
        private String title;
        private String description;
        private Long unitId;
        private Integer sortOrder;
    }
}
