package org.zerock.projecttraveler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "last_position_sec")
    @Builder.Default
    private Integer lastPositionSec = 0;

    @Column(name = "watched_sec")
    @Builder.Default
    private Integer watchedSec = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 진행률 계산 (0~100)
    public int getProgressPercent() {
        if (completed) return 100;
        if (lesson == null || lesson.getDurationSec() == null || lesson.getDurationSec() == 0) {
            return 0;
        }
        int percent = (int) ((watchedSec * 100.0) / lesson.getDurationSec());
        return Math.min(percent, 99); // 완료 전까지는 최대 99%
    }

    // 상태 표시 (미시작/진행중/완료)
    public String getStatusDisplay() {
        if (completed) return "완료";
        if (watchedSec == null || watchedSec == 0) return "미시작";
        return "진행중";
    }
}
