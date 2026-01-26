package org.zerock.projecttraveler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lesson")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private CourseUnit unit;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "duration_sec")
    @Builder.Default
    private Integer durationSec = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "video_type", length = 20)
    @Builder.Default
    private VideoType videoType = VideoType.NONE;

    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    @Column(name = "is_preview")
    @Builder.Default
    private Boolean isPreview = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum VideoType {
        NONE,       // 영상 없음
        MP4,        // 로컬 MP4 파일
        HLS,        // HLS 스트리밍
        YOUTUBE     // YouTube 임베드
    }

    // 시간 포맷 (예: "15분")
    public String getFormattedDuration() {
        if (durationSec == null || durationSec == 0) return "미정";
        int minutes = durationSec / 60;
        int seconds = durationSec % 60;
        if (minutes > 0 && seconds > 0) {
            return String.format("%d분 %d초", minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d분", minutes);
        } else {
            return String.format("%d초", seconds);
        }
    }

    // 강의 번호 표시 (예: "1강")
    public String getLessonNumberDisplay() {
        return sortOrder + "강";
    }
}
