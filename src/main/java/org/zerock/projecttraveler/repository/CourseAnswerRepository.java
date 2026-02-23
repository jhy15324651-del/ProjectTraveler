package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.CourseAnswer;

import java.util.List;

public interface CourseAnswerRepository extends JpaRepository<CourseAnswer, Long> {

    @Query("SELECT a FROM CourseAnswer a JOIN FETCH a.user WHERE a.question.id = :questionId ORDER BY a.createdAt ASC")
    List<CourseAnswer> findByQuestionIdWithUser(@Param("questionId") Long questionId);

    long countByQuestionId(Long questionId);
}
