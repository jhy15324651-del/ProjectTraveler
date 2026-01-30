package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.Certificate;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    List<Certificate> findByUserIdOrderByIssuedAtDesc(Long userId);

    Optional<Certificate> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    Optional<Certificate> findByCertificateNumber(String certificateNumber);

    @Query("SELECT c FROM Certificate c JOIN FETCH c.course WHERE c.user.id = :userId ORDER BY c.issuedAt DESC")
    List<Certificate> findByUserIdWithCourse(@Param("userId") Long userId);

    int countByUserId(Long userId);
}
