package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.LessonProgress;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<LessonProgress> findByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT lp FROM LessonProgress lp JOIN FETCH lp.lesson WHERE lp.user.id = :userId AND lp.course.id = :courseId")
    List<LessonProgress> findByUserIdAndCourseIdWithLesson(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // 완료한 레슨 수
    @Query("SELECT COUNT(lp) FROM LessonProgress lp WHERE lp.user.id = :userId AND lp.course.id = :courseId AND lp.completed = true")
    long countCompletedByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // 총 누적 시청 시간 (초)
    @Query("SELECT COALESCE(SUM(lp.watchedSec), 0) FROM LessonProgress lp WHERE lp.user.id = :userId")
    long sumWatchedSecByUserId(@Param("userId") Long userId);

    // 특정 강좌의 총 시청 시간
    @Query("SELECT COALESCE(SUM(lp.watchedSec), 0) FROM LessonProgress lp WHERE lp.user.id = :userId AND lp.course.id = :courseId")
    long sumWatchedSecByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // 최근 학습 활동
    @Query("SELECT lp FROM LessonProgress lp JOIN FETCH lp.lesson JOIN FETCH lp.course WHERE lp.user.id = :userId AND lp.completed = true ORDER BY lp.completedAt DESC")
    List<LessonProgress> findRecentCompletedByUserId(@Param("userId") Long userId);

    // 사용자별 완료한 총 레슨 수
    @Query("SELECT COUNT(lp) FROM LessonProgress lp WHERE lp.user.id = :userId AND lp.completed = true")
    long countCompletedByUserId(@Param("userId") Long userId);
}
