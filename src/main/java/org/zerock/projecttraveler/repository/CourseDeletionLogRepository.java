package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.projecttraveler.entity.CourseDeletionLog;

import java.time.LocalDateTime;
import java.util.List;

public interface CourseDeletionLogRepository extends JpaRepository<CourseDeletionLog, Long> {

    // 삭제한 사람으로 조회
    List<CourseDeletionLog> findByDeletedByOrderByDeletedAtDesc(String deletedBy);

    // 기간별 삭제 이력 조회
    List<CourseDeletionLog> findByDeletedAtBetweenOrderByDeletedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    // 최근 삭제 이력 조회
    List<CourseDeletionLog> findTop10ByOrderByDeletedAtDesc();

    // 강좌 ID로 조회 (해당 강좌가 삭제된 적 있는지)
    List<CourseDeletionLog> findByCourseIdOrderByDeletedAtDesc(Long courseId);
}