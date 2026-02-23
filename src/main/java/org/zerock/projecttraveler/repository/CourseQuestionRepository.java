package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.CourseQuestion;

import java.util.List;
import java.util.Optional;

public interface CourseQuestionRepository extends JpaRepository<CourseQuestion, Long> {

    @Query("SELECT q FROM CourseQuestion q JOIN FETCH q.user WHERE q.course.id = :courseId ORDER BY q.createdAt DESC")
    List<CourseQuestion> findByCourseIdWithUser(@Param("courseId") Long courseId);

    @Query("SELECT q FROM CourseQuestion q JOIN FETCH q.user LEFT JOIN FETCH q.answers a LEFT JOIN FETCH a.user WHERE q.id = :id")
    Optional<CourseQuestion> findByIdWithUserAndAnswers(@Param("id") Long id);

    @Query("SELECT q FROM CourseQuestion q JOIN FETCH q.user JOIN FETCH q.course ORDER BY q.createdAt DESC")
    List<CourseQuestion> findAllWithUserAndCourse();
}
