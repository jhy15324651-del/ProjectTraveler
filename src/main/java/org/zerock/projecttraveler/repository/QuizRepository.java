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

    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions qs LEFT JOIN FETCH qs.options " +
           "WHERE q.id = :quizId AND q.active = true")
    Optional<Quiz> findByIdWithQuestionsAndOptions(@Param("quizId") Long quizId);

    boolean existsByCourseIdAndActiveTrue(Long courseId);
}
