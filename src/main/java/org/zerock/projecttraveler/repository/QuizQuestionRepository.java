package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.QuizQuestion;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuizIdOrderBySortOrderAsc(Long quizId);

    @Query("SELECT q FROM QuizQuestion q LEFT JOIN FETCH q.options WHERE q.quiz.id = :quizId ORDER BY q.sortOrder ASC")
    List<QuizQuestion> findByQuizIdWithOptions(@Param("quizId") Long quizId);

    int countByQuizId(Long quizId);
}
