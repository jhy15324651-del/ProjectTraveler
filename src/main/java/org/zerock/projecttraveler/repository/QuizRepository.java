package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.Quiz;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByCourseIdAndActiveTrue(Long courseId);

    Optional<Quiz> findFirstByCourseIdAndActiveTrue(Long courseId);

    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.id = :quizId")
    Optional<Quiz> findByIdWithQuestions(@Param("quizId") Long quizId);

    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions qs " +
           "WHERE q.id = :quizId AND q.active = true")
    Optional<Quiz> findByIdWithQuestionsAndOptions(@Param("quizId") Long quizId);

    boolean existsByCourseIdAndActiveTrue(Long courseId);

    // 코스별 퀴즈 리스트 조회 (ID 오름차순)
    List<Quiz> findByCourseIdAndActiveTrueOrderByIdAsc(Long courseId);

    // 레슨별 퀴즈 조회
    Optional<Quiz> findFirstByLessonIdAndActiveTrue(Long lessonId);

    List<Quiz> findByLessonIdAndActiveTrue(Long lessonId);

    // 레슨별 퀴즈 리스트 조회 (ID 오름차순)
    List<Quiz> findByLessonIdAndActiveTrueOrderByIdAsc(Long lessonId);

    boolean existsByLessonIdAndActiveTrue(Long lessonId);

    // 강좌의 모든 퀴즈 조회 (레슨 정보 포함)
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.lesson WHERE q.course.id = :courseId AND q.active = true ORDER BY q.lesson.sortOrder ASC NULLS FIRST")
    List<Quiz> findAllByCourseIdWithLesson(@Param("courseId") Long courseId);
}
