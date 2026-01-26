package org.zerock.projecttraveler.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.entity.Course;
import org.zerock.projecttraveler.entity.CourseUnit;
import org.zerock.projecttraveler.entity.Lesson;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.CourseService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/courses")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;

    @Value("${app.upload.video-path:C:/lms-uploads/videos}")
    private String videoUploadPath;

    /**
     * 강좌 관리 목록 페이지
     */
    @GetMapping
    public String listPage(Model model) {
        List<Course> courses = courseService.findAllActiveCourses();

        model.addAttribute("courses", courses);
        model.addAttribute("activePage", "admin-courses");
        model.addAttribute("username", SecurityUtils.getCurrentUserDetails()
                .map(u -> u.getFullName()).orElse("관리자"));
        model.addAttribute("isAdmin", true);

        return "admin/course-list";
    }

    /**
     * 강좌 등록 페이지
     */
    @GetMapping("/new")
    public String newPage(Model model) {
        model.addAttribute("activePage", "admin-courses");
        model.addAttribute("username", SecurityUtils.getCurrentUserDetails()
                .map(u -> u.getFullName()).orElse("관리자"));
        model.addAttribute("isAdmin", true);
        model.addAttribute("categories", Course.Category.values());
        model.addAttribute("levels", Course.Level.values());
        model.addAttribute("policies", Course.EnrollPolicy.values());

        return "admin/course-form";
    }

    /**
     * 강좌 수정 페이지
     */
    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        model.addAttribute("course", course);
        model.addAttribute("activePage", "admin-courses");
        model.addAttribute("username", SecurityUtils.getCurrentUserDetails()
                .map(u -> u.getFullName()).orElse("관리자"));
        model.addAttribute("isAdmin", true);
        model.addAttribute("categories", Course.Category.values());
        model.addAttribute("levels", Course.Level.values());
        model.addAttribute("policies", Course.EnrollPolicy.values());

        return "admin/course-form";
    }

    /**
     * 강좌 상세 (레슨 관리 포함)
     */
    @GetMapping("/{id}")
    public String detailPage(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        List<CourseUnit> units = courseService.findUnitsWithLessons(id);
        List<Lesson> lessons = courseService.findLessonsByCourseId(id);

        model.addAttribute("course", course);
        model.addAttribute("units", units);
        model.addAttribute("lessons", lessons);
        model.addAttribute("activePage", "admin-courses");
        model.addAttribute("username", SecurityUtils.getCurrentUserDetails()
                .map(u -> u.getFullName()).orElse("관리자"));
        model.addAttribute("isAdmin", true);

        return "admin/course-detail";
    }

    /**
     * 강좌 생성 API
     */
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<ApiResponse<Course>> createCourse(@Valid @RequestBody CourseRequest request) {
        try {
            Course course = Course.builder()
                    .title(request.getTitle())
                    .shortDesc(request.getShortDesc())
                    .fullDesc(request.getFullDesc())
                    .thumbnailUrl(request.getThumbnailUrl())
                    .category(request.getCategory())
                    .level(request.getLevel())
                    .enrollPolicy(request.getEnrollPolicy())
                    .build();

            Course saved = courseService.createCourse(course);
            return ResponseEntity.ok(ApiResponse.success("강좌가 등록되었습니다.", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 강좌 수정 API
     */
    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Course>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request) {
        try {
            Course course = Course.builder()
                    .title(request.getTitle())
                    .shortDesc(request.getShortDesc())
                    .fullDesc(request.getFullDesc())
                    .thumbnailUrl(request.getThumbnailUrl())
                    .category(request.getCategory())
                    .level(request.getLevel())
                    .enrollPolicy(request.getEnrollPolicy())
                    .build();

            Course updated = courseService.updateCourse(id, course);
            return ResponseEntity.ok(ApiResponse.success("강좌가 수정되었습니다.", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 유닛 생성 API
     */
    @PostMapping("/api/{courseId}/units")
    @ResponseBody
    public ResponseEntity<ApiResponse<CourseUnit>> createUnit(
            @PathVariable Long courseId,
            @Valid @RequestBody UnitRequest request) {
        try {
            CourseUnit unit = courseService.createUnit(courseId, request.getTitle(), request.getSortOrder());
            return ResponseEntity.ok(ApiResponse.success("유닛이 추가되었습니다.", unit));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 레슨 생성 API
     */
    @PostMapping("/api/{courseId}/lessons")
    @ResponseBody
    public ResponseEntity<ApiResponse<Lesson>> createLesson(
            @PathVariable Long courseId,
            @Valid @RequestBody LessonRequest request) {
        try {
            Lesson lesson = courseService.createLesson(
                    courseId,
                    request.getUnitId(),
                    request.getTitle(),
                    request.getSortOrder(),
                    request.getDurationSec() != null ? request.getDurationSec() : 0
            );
            return ResponseEntity.ok(ApiResponse.success("레슨이 추가되었습니다.", lesson));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 레슨 영상 업로드 API
     */
    @PostMapping("/api/lessons/{lessonId}/video")
    @ResponseBody
    public ResponseEntity<ApiResponse<Lesson>> uploadVideo(
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "durationSec", required = false) Integer durationSec) {
        try {
            // 파일 저장
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".mp4";

            String savedFilename = UUID.randomUUID() + extension;

            Path uploadDir = Paths.get(videoUploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path filePath = uploadDir.resolve(savedFilename);
            file.transferTo(filePath.toFile());

            // 레슨 업데이트
            String videoUrl = "/uploads/videos/" + savedFilename;
            Lesson lesson = courseService.updateLessonVideo(lessonId, Lesson.VideoType.MP4, videoUrl, durationSec);

            return ResponseEntity.ok(ApiResponse.success("영상이 업로드되었습니다.", lesson));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("파일 업로드 실패: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Data
    public static class CourseRequest {
        @NotBlank(message = "강좌명은 필수입니다.")
        private String title;
        private String shortDesc;
        private String fullDesc;
        private String thumbnailUrl;
        private Course.Category category;
        private Course.Level level;
        private Course.EnrollPolicy enrollPolicy;
    }

    @Data
    public static class UnitRequest {
        @NotBlank(message = "유닛명은 필수입니다.")
        private String title;
        private int sortOrder;
    }

    @Data
    public static class LessonRequest {
        @NotBlank(message = "레슨명은 필수입니다.")
        private String title;
        private Long unitId;
        private int sortOrder;
        private Integer durationSec;
    }
}
