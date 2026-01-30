package org.zerock.projecttraveler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_attempt")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private Integer correctCount = 0;

    @Column(name = "score_percent", nullable = false)
    @Builder.Default
    private Integer scorePercent = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean passed = false;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuizAnswer> answers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    public void calculateScore() {
        if (totalQuestions == null || totalQuestions == 0) {
            this.scorePercent = 0;
            return;
        }
        this.scorePercent = (correctCount * 100) / totalQuestions;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public String getResultDisplay() {
        if (!isCompleted()) return "진행 중";
        return passed ? "합격 (" + scorePercent + "%)" : "불합격 (" + scorePercent + "%)";
    }
}
