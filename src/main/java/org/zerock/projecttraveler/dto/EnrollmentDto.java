package org.zerock.projecttraveler.dto;

import lombok.*;
import org.zerock.projecttraveler.entity.CourseEnrollment;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDto {

    private Long id;
    private Long userId;
    private String username;
    private String userFullName;

    private Long courseId;
    private String courseTitle;
    private String courseCategory;
    private String courseThumbnailUrl;

    private String status;
    private String statusDisplayName;
    private String source;

    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime assignedAt;
    private String assignedByAdminName;
    private String note;

    private LocalDateTime lastAccessedAt;
    private Long lastLessonId;
    private String lastLessonTitle;

    // 진도 정보
    private int progressPercent;
    private int completedLessonCount;
    private int totalLessonCount;
    private long totalWatchedSec;
    private String totalWatchedFormatted;

    public static EnrollmentDto from(CourseEnrollment enrollment) {
        return EnrollmentDto.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUser().getId())
                .username(enrollment.getUser().getUsername())
                .userFullName(enrollment.getUser().getFullName())
                .courseId(enrollment.getCourse().getId())
                .courseTitle(enrollment.getCourse().getTitle())
                .courseCategory(enrollment.getCourse().getCategoryDisplayName())
                .courseThumbnailUrl(enrollment.getCourse().getThumbnailUrl())
                .status(enrollment.getStatus().name())
                .statusDisplayName(enrollment.getStatusDisplayName())
                .source(enrollment.getSource().name())
                .requestedAt(enrollment.getRequestedAt())
                .approvedAt(enrollment.getApprovedAt())
                .assignedAt(enrollment.getAssignedAt())
                .assignedByAdminName(enrollment.getAssignedByAdmin() != null ?
                        enrollment.getAssignedByAdmin().getFullName() : null)
                .note(enrollment.getNote())
                .lastAccessedAt(enrollment.getLastAccessedAt())
                .lastLessonId(enrollment.getLastLesson() != null ? enrollment.getLastLesson().getId() : null)
                .lastLessonTitle(enrollment.getLastLesson() != null ? enrollment.getLastLesson().getTitle() : null)
                .build();
    }
}
