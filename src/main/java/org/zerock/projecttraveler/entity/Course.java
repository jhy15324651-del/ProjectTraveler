package org.zerock.projecttraveler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "short_desc", columnDefinition = "TEXT")
    private String shortDesc;

    @Column(name = "full_desc", columnDefinition = "TEXT")
    private String fullDesc;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Level level = Level.BEGINNER;

    @Column(name = "total_duration_sec")
    @Builder.Default
    private Integer totalDurationSec = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "enroll_policy", length = 20)
    @Builder.Default
    private EnrollPolicy enrollPolicy = EnrollPolicy.SELF;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<CourseUnit> units = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<Lesson> lessons = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Category {
        LANGUAGE, CULTURE, TRAVEL
    }

    public enum Level {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    public enum EnrollPolicy {
        SELF,           // 즉시 수강 가능
        APPROVAL,       // 관리자 승인 필요
        ASSIGN_ONLY     // 관리자 배정만 가능
    }

    // 총 레슨 수 계산
    public int getTotalLessonCount() {
        return lessons != null ? lessons.size() : 0;
    }

    // 카테고리 한글명
    public String getCategoryDisplayName() {
        if (category == null) return "";
        return switch (category) {
            case LANGUAGE -> "일본어";
            case CULTURE -> "문화";
            case TRAVEL -> "여행";
        };
    }

    // 레벨 한글명
    public String getLevelDisplayName() {
        if (level == null) return "";
        return switch (level) {
            case BEGINNER -> "입문";
            case INTERMEDIATE -> "중급";
            case ADVANCED -> "고급";
        };
    }

    // 총 시간 포맷 (예: "약 3시간")
    public String getFormattedDuration() {
        if (totalDurationSec == null || totalDurationSec == 0) return "미정";
        int hours = totalDurationSec / 3600;
        int minutes = (totalDurationSec % 3600) / 60;
        if (hours > 0 && minutes > 0) {
            return String.format("약 %d시간 %d분", hours, minutes);
        } else if (hours > 0) {
            return String.format("약 %d시간", hours);
        } else {
            return String.format("약 %d분", minutes);
        }
    }
}