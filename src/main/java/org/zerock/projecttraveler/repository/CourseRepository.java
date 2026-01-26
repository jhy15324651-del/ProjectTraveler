package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.Course;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByActiveTrueOrderByCreatedAtDesc();

    List<Course> findByCategoryAndActiveTrueOrderByCreatedAtDesc(Course.Category category);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.units u LEFT JOIN FETCH u.lessons WHERE c.id = :id")
    Optional<Course> findByIdWithUnitsAndLessons(@Param("id") Long id);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.lessons WHERE c.id = :id")
    Optional<Course> findByIdWithLessons(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Course c " +
           "LEFT JOIN FETCH c.units " +
           "WHERE c.active = true " +
           "ORDER BY c.createdAt DESC")
    List<Course> findAllActiveWithUnits();

    long countByActiveTrue();
}
