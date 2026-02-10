package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.QuizAttempt;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByUserIdAndQuizIdOrderByStartedAtDesc(Long userId, Long quizId);

    Optional<QuizAttempt> findFirstByUserIdAndQuizIdAndPassedTrueOrderByCompletedAtDesc(Long userId, Long quizId);

    @Query("SELECT MAX(a.scorePercent) FROM QuizAttempt a WHERE a.user.id = :userId AND a.quiz.id = :quizId AND a.completedAt IS NOT NULL")
    Optional<Integer> findBestScoreByUserIdAndQuizId(@Param("userId") Long userId, @Param("quizId") Long quizId);

    @Query("SELECT a FROM QuizAttempt a WHERE a.user.id = :userId AND a.quiz.course.id = :courseId AND a.passed = true")
    List<QuizAttempt> findPassedAttemptsByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("SELECT MAX(a.scorePercent) FROM QuizAttempt a WHERE a.user.id = :userId AND a.quiz.course.id = :courseId AND a.completedAt IS NOT NULL")
    Optional<Integer> findBestScoreByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    boolean existsByUserIdAndQuizIdAndPassedTrue(Long userId, Long quizId);

    int countByUserIdAndQuizId(Long userId, Long quizId);

    /**
     * 특정 사이클 내 시도 횟수 조회
     */
    int countByUserIdAndQuizIdAndCycle(Long userId, Long quizId, Integer cycle);

    /**
     * 특정 사이클 내 시도 목록 조회
     */
    List<QuizAttempt> findByUserIdAndQuizIdAndCycleOrderByAttemptNoAsc(Long userId, Long quizId, Integer cycle);

    /**
     * 특정 사이클의 마지막 시도 조회
     */
    Optional<QuizAttempt> findFirstByUserIdAndQuizIdAndCycleOrderByAttemptNoDesc(Long userId, Long quizId, Integer cycle);

    /**
     * 특정 시도 조회 (사이클 + 시도번호)
     */
    Optional<QuizAttempt> findByUserIdAndQuizIdAndCycleAndAttemptNo(Long userId, Long quizId, Integer cycle, Integer attemptNo);

    /**
     * 특정 사이클에서 합격 여부 확인
     */
    boolean existsByUserIdAndQuizIdAndCycleAndPassedTrue(Long userId, Long quizId, Integer cycle);
}
