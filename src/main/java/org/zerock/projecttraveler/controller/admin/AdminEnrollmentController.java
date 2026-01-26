package org.zerock.projecttraveler.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.dto.EnrollmentDto;
import org.zerock.projecttraveler.entity.Course;
import org.zerock.projecttraveler.entity.CourseEnrollment;
import org.zerock.projecttraveler.entity.User;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/enrollments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminEnrollmentController {

    private final EnrollmentAdminService enrollmentAdminService;
    private final EnrollmentService enrollmentService;
    private final CourseService courseService;
    private final UserService userService;

    /**
     * 수강 승인 대기 목록 페이지
     */
    @GetMapping("/requests")
    public String requestsPage(Model model) {
        List<CourseEnrollment> requests = enrollmentAdminService.findAllRequested();
        List<EnrollmentDto> enrollments = requests.stream()
                .map(EnrollmentDto::from)
                .collect(Collectors.toList());

        model.addAttribute("enrollments", enrollments);
        model.addAttribute("activePage", "admin-requests");
        model.addAttribute("username", SecurityUtils.getCurrentUserDetails()
                .map(u -> u.getFullName()).orElse("관리자"));
        model.addAttribute("isAdmin", true);

        return "admin/enrollment-requests";
    }

    /**
     * 수강 배정 페이지
     */
    @GetMapping("/assign")
    public String assignPage(Model model) {
        List<Course> courses = courseService.findAllActiveCourses();
        List<User> users = userService.findAllUsers();

        model.addAttribute("courses", courses);
        model.addAttribute("users", users);
        model.addAttribute("activePage", "admin-assign");
        model.addAttribute("username", SecurityUtils.getCurrentUserDetails()
                .map(u -> u.getFullName()).orElse("관리자"));
        model.addAttribute("isAdmin", true);

        return "admin/enrollment-assign";
    }

    /**
     * 수강 승인 API
     */
    @PostMapping("/api/{id}/approve")
    @ResponseBody
    public ResponseEntity<ApiResponse<EnrollmentDto>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) NoteRequest request) {
        try {
            CourseEnrollment enrollment = enrollmentAdminService.approve(id, request != null ? request.getNote() : null);
            return ResponseEntity.ok(ApiResponse.success("수강이 승인되었습니다.", EnrollmentDto.from(enrollment)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 수강 반려 API
     */
    @PostMapping("/api/{id}/reject")
    @ResponseBody
    public ResponseEntity<ApiResponse<EnrollmentDto>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) NoteRequest request) {
        try {
            CourseEnrollment enrollment = enrollmentAdminService.reject(id, request != null ? request.getNote() : null);
            return ResponseEntity.ok(ApiResponse.success("수강이 반려되었습니다.", EnrollmentDto.from(enrollment)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 수강 배정 API
     */
    @PostMapping("/api/assign")
    @ResponseBody
    public ResponseEntity<ApiResponse<Integer>> assign(@Valid @RequestBody AssignRequest request) {
        Long adminId = SecurityUtils.getCurrentUserIdOrThrow();

        try {
            int count = enrollmentAdminService.assignToMultipleUsers(
                    adminId,
                    request.getUserIds(),
                    request.getCourseId(),
                    request.getNote()
            );
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("%d명에게 강좌가 배정되었습니다.", count), count));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Data
    public static class NoteRequest {
        private String note;
    }

    @Data
    public static class AssignRequest {
        @NotNull(message = "강좌 ID는 필수입니다.")
        private Long courseId;

        @NotEmpty(message = "배정할 사용자를 선택해주세요.")
        private List<Long> userIds;

        private String note;
    }
}
