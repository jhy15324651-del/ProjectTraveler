package org.zerock.projecttraveler.dto;

import lombok.*;
import org.zerock.projecttraveler.entity.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailDto {

    private Long id;
    private String title;
    private String shortDesc;
    private String fullDesc;
    private String thumbnailUrl;
    private String category;
    private String categoryDisplayName;
    private String level;
    private String levelDisplayName;
    private String formattedDuration;
    private int totalLessonCount;
    private int totalUnitCount;

    // 수강 정보
    private EnrollmentInfo enrollmentInfo;

    // 진도 정보
    private ProgressInfo progressInfo;

    // 유닛 및 레슨 목록
    private List<UnitDto> units;

    // 이어서 학습할 레슨
    private LessonDto continueLesson;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentInfo {
        private Long enrollmentId;
        private String status;
        private String statusDisplayName;
        private boolean accessible;
        private String enrollPolicy;
        private LocalDateTime lastAccessedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressInfo {
        private int completedLessonCount;
        private int totalLessonCount;
        private int progressPercent;
        private long totalWatchedSec;
        private String totalWatchedFormatted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitDto {
        private Long id;
        private String title;
        private int sortOrder;
        private int lessonCount;
        private String formattedDuration;
        private String status; // 완료, 진행중, 미시작
        private List<LessonDto> lessons;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonDto {
        private Long id;
        private String title;
        private int sortOrder;
        private String formattedDuration;
        private String videoType;
        private String videoUrl;
        private boolean isPreview;

        // 진도 정보
        private String status; // 완료, 진행중, 미시작
        private int progressPercent;
        private int lastPositionSec;
        private boolean completed;
    }

    // Entity를 DTO로 변환하는 정적 메서드
    public static CourseDetailDto from(Course course) {
        return CourseDetailDto.builder()
                .id(course.getId())
                .title(course.getTitle())
                .shortDesc(course.getShortDesc())
                .fullDesc(course.getFullDesc())
                .thumbnailUrl(course.getThumbnailUrl())
                .category(course.getCategory() != null ? course.getCategory().name() : null)
                .categoryDisplayName(course.getCategoryDisplayName())
                .level(course.getLevel() != null ? course.getLevel().name() : null)
                .levelDisplayName(course.getLevelDisplayName())
                .formattedDuration(course.getFormattedDuration())
                .totalLessonCount(course.getTotalLessonCount())
                .totalUnitCount(course.getUnits() != null ? course.getUnits().size() : 0)
                .build();
    }
}
