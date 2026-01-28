package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.CourseEnrollment;
import org.zerock.projecttraveler.entity.CourseEnrollment.Status;

import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    Optional<CourseEnrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    List<CourseEnrollment> findByUserIdOrderByLastAccessedAtDesc(Long userId);

    @Query("SELECT e FROM CourseEnrollment e JOIN FETCH e.course WHERE e.user.id = :userId AND e.status IN :statuses ORDER BY e.lastAccessedAt DESC")
    List<CourseEnrollment> findByUserIdAndStatusInWithCourse(@Param("userId") Long userId, @Param("statuses") List<Status> statuses);

    // 학습 가능한 수강 목록 (APPROVED, ASSIGNED, COMPLETED, PAUSED)
    @Query("SELECT e FROM CourseEnrollment e JOIN FETCH e.course WHERE e.user.id = :userId AND e.status IN ('APPROVED', 'ASSIGNED', 'COMPLETED', 'PAUSED') ORDER BY e.lastAccessedAt DESC NULLS LAST")
    List<CourseEnrollment> findAccessibleByUserId(@Param("userId") Long userId);

    // 진행 중인 수강 목록 (APPROVED, ASSIGNED, PAUSED)
    @Query("SELECT e FROM CourseEnrollment e JOIN FETCH e.course WHERE e.user.id = :userId AND e.status IN ('APPROVED', 'ASSIGNED', 'PAUSED') ORDER BY e.lastAccessedAt DESC NULLS LAST")
    List<CourseEnrollment> findInProgressByUserId(@Param("userId") Long userId);

    // 완료된 수강 목록
    @Query("SELECT e FROM CourseEnrollment e JOIN FETCH e.course WHERE e.user.id = :userId AND e.status = 'COMPLETED' ORDER BY e.updatedAt DESC")
    List<CourseEnrollment> findCompletedByUserId(@Param("userId") Long userId);

    // 승인 대기 목록 (관리자용)
    @Query("SELECT e FROM CourseEnrollment e JOIN FETCH e.user JOIN FETCH e.course WHERE e.status = 'REQUESTED' ORDER BY e.requestedAt ASC")
    List<CourseEnrollment> findAllRequestedWithUserAndCourse();

    // 사용자의 수강 중인 강좌 수
    @Query("SELECT COUNT(e) FROM CourseEnrollment e WHERE e.user.id = :userId AND e.status IN ('APPROVED', 'ASSIGNED', 'PAUSED')")
    long countInProgressByUserId(@Param("userId") Long userId);

    // 사용자의 완료한 강좌 수
    @Query("SELECT COUNT(e) FROM CourseEnrollment e WHERE e.user.id = :userId AND e.status = 'COMPLETED'")
    long countCompletedByUserId(@Param("userId") Long userId);

    // 사용자의 승인 대기 목록
    @Query("SELECT e FROM CourseEnrollment e JOIN FETCH e.course WHERE e.user.id = :userId AND e.status = 'REQUESTED' ORDER BY e.requestedAt DESC")
    List<CourseEnrollment> findPendingByUserId(@Param("userId") Long userId);
}
