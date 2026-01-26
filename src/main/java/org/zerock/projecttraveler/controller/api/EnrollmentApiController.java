package org.zerock.projecttraveler.controller.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.dto.EnrollmentDto;
import org.zerock.projecttraveler.entity.CourseEnrollment;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.EnrollmentService;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentApiController {

    private final EnrollmentService enrollmentService;

    /**
     * 수강 신청
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<EnrollmentDto>> requestEnrollment(@Valid @RequestBody EnrollmentRequest request) {
        // 중요: userId는 클라이언트가 아닌 서버에서 SecurityContext로 강제
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        try {
            CourseEnrollment enrollment = enrollmentService.requestEnrollment(userId, request.getCourseId());
            EnrollmentDto dto = EnrollmentDto.from(enrollment);

            String message = switch (enrollment.getStatus()) {
                case APPROVED -> "수강 신청이 완료되었습니다. 바로 학습을 시작할 수 있습니다.";
                case REQUESTED -> "수강 신청이 접수되었습니다. 관리자 승인 후 학습이 가능합니다.";
                default -> "수강 신청이 처리되었습니다.";
            };

            return ResponseEntity.ok(ApiResponse.success(message, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 수강 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<EnrollmentDto>> getEnrollmentStatus(@RequestParam Long courseId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        return enrollmentService.findEnrollment(userId, courseId)
                .map(enrollment -> {
                    EnrollmentDto dto = enrollmentService.toEnrollmentDtoWithProgress(enrollment);
                    return ResponseEntity.ok(ApiResponse.success(dto));
                })
                .orElse(ResponseEntity.ok(ApiResponse.success("수강 신청 내역이 없습니다.", null)));
    }

    @Data
    public static class EnrollmentRequest {
        @NotNull(message = "강좌 ID는 필수입니다.")
        private Long courseId;
    }
}
