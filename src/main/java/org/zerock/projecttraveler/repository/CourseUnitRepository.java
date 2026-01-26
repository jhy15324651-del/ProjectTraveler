package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.CourseUnit;

import java.util.List;

public interface CourseUnitRepository extends JpaRepository<CourseUnit, Long> {

    List<CourseUnit> findByCourseIdOrderBySortOrderAsc(Long courseId);

    @Query("SELECT u FROM CourseUnit u LEFT JOIN FETCH u.lessons WHERE u.course.id = :courseId ORDER BY u.sortOrder ASC")
    List<CourseUnit> findByCourseIdWithLessons(@Param("courseId") Long courseId);

    int countByCourseId(Long courseId);
}
