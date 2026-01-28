package org.zerock.projecttraveler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 강좌 삭제 이력 엔티티
 * 누가, 언제, 무엇을, 왜 삭제했는지 기록
 */
@Entity
@Table(name = "course_deletion_log")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDeletionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 삭제된 강좌 ID (원본 참조용)
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    // 삭제된 강좌 제목 (기록 보존)
    @Column(name = "course_title", nullable = false, length = 200)
    private String courseTitle;

    // 삭제된 강좌 카테고리
    @Enumerated(EnumType.STRING)
    @Column(name = "course_category", length = 50)
    private Course.Category courseCategory;

    // 삭제된 강좌 레벨
    @Enumerated(EnumType.STRING)
    @Column(name = "course_level", length = 20)
    private Course.Level courseLevel;

    // 삭제된 강좌의 레슨 수
    @Column(name = "lesson_count")
    private Integer lessonCount;

    // 삭제 이유 (왜)
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    // 삭제한 사람 (누가)
    @Column(name = "deleted_by", nullable = false, length = 100)
    private String deletedBy;

    // 삭제한 사람 ID
    @Column(name = "deleted_by_user_id")
    private Long deletedByUserId;

    // 삭제 시간 (언제)
    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt;

    // 강좌 원본 생성일
    @Column(name = "course_created_at")
    private LocalDateTime courseCreatedAt;

    @PrePersist
    protected void onCreate() {
        if (deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
    }
}