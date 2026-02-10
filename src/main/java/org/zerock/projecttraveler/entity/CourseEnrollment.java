package org.zerock.projecttraveler.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_enrollment",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Source source = Source.USER_REQUEST;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_admin_id")
    private User assignedByAdmin;

    @Column(columnDefinition = "TEXT")
    private String note;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_lesson_id")
    private Lesson lastLesson;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    /**
     * 퀴즈 상태 (NOT_STARTED, IN_PROGRESS, RETRY_ALLOWED, PASSED, RETAKE_REQUIRED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_status", length = 30)
    @Builder.Default
    private QuizStatus quizStatus = QuizStatus.NOT_STARTED;

    /**
     * 현재 퀴즈 사이클 (재수강 완료 시 증가)
     */
    @Column(name = "quiz_cycle", nullable = false)
    @Builder.Default
    private Integer quizCycle = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (requestedAt == null && source == Source.USER_REQUEST) {
            requestedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        REQUESTED,      // 신청됨 (승인 대기)
        APPROVED,       // 승인됨
        REJECTED,       // 반려됨
        ASSIGNED,       // 관리자 배정
        COMPLETED,      // 수강 완료
        PAUSED          // 일시 정지
    }

    public enum Source {
        USER_REQUEST,   // 사용자 직접 신청
        ADMIN_ASSIGN    // 관리자 배정
    }

    /**
     * 퀴즈 응시 상태
     */
    public enum QuizStatus {
        NOT_STARTED,      // 퀴즈 미응시
        IN_PROGRESS,      // 응시 가능 (1차 시험 전)
        RETRY_ALLOWED,    // 1차 실패, 2차 응시 가능
        PASSED,           // 합격
        RETAKE_REQUIRED   // 2차 실패, 재수강 필요 (퀴즈 잠금)
    }

    // 학습 가능한 상태인지 확인
    public boolean isAccessible() {
        return status == Status.APPROVED ||
               status == Status.ASSIGNED ||
               status == Status.COMPLETED ||
               status == Status.PAUSED;
    }

    // 진행 중 상태인지 확인
    public boolean isInProgress() {
        return status == Status.APPROVED ||
               status == Status.ASSIGNED ||
               status == Status.PAUSED;
    }

    // 상태 한글명
    public String getStatusDisplayName() {
        return switch (status) {
            case REQUESTED -> "승인 대기";
            case APPROVED -> "수강 중";
            case REJECTED -> "반려됨";
            case ASSIGNED -> "수강 중";
            case COMPLETED -> "완료";
            case PAUSED -> "일시 정지";
        };
    }
}
