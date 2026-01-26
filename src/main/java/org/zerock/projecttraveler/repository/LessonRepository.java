package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.Lesson;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdOrderBySortOrderAsc(Long courseId);

    List<Lesson> findByUnitIdOrderBySortOrderAsc(Long unitId);

    @Query("SELECT l FROM Lesson l WHERE l.course.id = :courseId ORDER BY l.sortOrder ASC")
    List<Lesson> findAllByCourseId(@Param("courseId") Long courseId);

    int countByCourseId(Long courseId);

    @Query("SELECT l FROM Lesson l WHERE l.course.id = :courseId AND l.sortOrder = 1")
    Optional<Lesson> findFirstLessonByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT l FROM Lesson l WHERE l.course.id = :courseId AND l.sortOrder > :currentOrder ORDER BY l.sortOrder ASC LIMIT 1")
    Optional<Lesson> findNextLesson(@Param("courseId") Long courseId, @Param("currentOrder") Integer currentOrder);

    @Query("SELECT l FROM Lesson l WHERE l.course.id = :courseId AND l.sortOrder < :currentOrder ORDER BY l.sortOrder DESC LIMIT 1")
    Optional<Lesson> findPreviousLesson(@Param("courseId") Long courseId, @Param("currentOrder") Integer currentOrder);
}
