package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.CourseResource;

import java.util.List;

public interface CourseResourceRepository extends JpaRepository<CourseResource, Long> {

    @Query("SELECT r FROM CourseResource r LEFT JOIN FETCH r.unit WHERE r.course.id = :courseId AND r.active = true ORDER BY r.unit.sortOrder ASC NULLS LAST, r.sortOrder ASC")
    List<CourseResource> findActiveByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT r FROM CourseResource r LEFT JOIN FETCH r.unit WHERE r.course.id = :courseId ORDER BY r.unit.sortOrder ASC NULLS LAST, r.sortOrder ASC")
    List<CourseResource> findAllByCourseIdWithUnit(@Param("courseId") Long courseId);
}
