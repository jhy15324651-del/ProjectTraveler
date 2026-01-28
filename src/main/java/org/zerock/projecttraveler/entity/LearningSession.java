package org.zerock.projecttraveler.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(name = "learning_session")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    // 세션 종료
    public void endSession() {
        if (endedAt == null) {
            endedAt = LocalDateTime.now();
            if (startedAt != null) {
                durationSec = (int) Duration.between(startedAt, endedAt).getSeconds();
            }
        }
    }

    // 진행 중인 세션인지 확인
    public boolean isActive() {
        return endedAt == null;
    }
}
