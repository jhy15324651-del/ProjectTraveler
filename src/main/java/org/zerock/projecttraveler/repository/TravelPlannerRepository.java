package org.zerock.projecttraveler.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.TravelPlanner;
import org.zerock.projecttraveler.entity.User;

import java.util.List;
import java.util.Optional;

public interface TravelPlannerRepository extends JpaRepository<TravelPlanner, Long> {

    // 사용자의 플래너 목록
    List<TravelPlanner> findByUserOrderByCreatedAtDesc(User user);

    List<TravelPlanner> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 공개된 플래너 목록 (최신순)
    List<TravelPlanner> findByVisibilityOrderByCreatedAtDesc(TravelPlanner.Visibility visibility);

    // 공개된 플래너 목록 (페이징)
    Page<TravelPlanner> findByVisibilityOrderByCreatedAtDesc(TravelPlanner.Visibility visibility, Pageable pageable);

    // 공개된 플래너 목록 (인기순 - 조회수)
    List<TravelPlanner> findByVisibilityOrderByViewCountDesc(TravelPlanner.Visibility visibility);

    // 공개된 플래너 목록 (좋아요순)
    List<TravelPlanner> findByVisibilityOrderByLikeCountDesc(TravelPlanner.Visibility visibility);

    // 플래너 상세 조회 (연관 엔티티 포함)
    @Query("SELECT DISTINCT p FROM TravelPlanner p " +
           "LEFT JOIN FETCH p.user " +
           "WHERE p.id = :id")
    Optional<TravelPlanner> findByIdWithUser(@Param("id") Long id);

    // 플래너 상세 조회 (일정 포함)
    @Query("SELECT DISTINCT p FROM TravelPlanner p " +
           "LEFT JOIN FETCH p.itineraries " +
           "WHERE p.id = :id")
    Optional<TravelPlanner> findByIdWithItineraries(@Param("id") Long id);

    // 여행지 검색 (공개된 플래너)
    @Query("SELECT p FROM TravelPlanner p " +
           "WHERE p.visibility = 'PUBLIC' " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.destination) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<TravelPlanner> searchPublicPlanners(@Param("keyword") String keyword);

    // 사용자가 접근 가능한 플래너 확인 (본인 또는 공유받은)
    @Query("SELECT p FROM TravelPlanner p " +
           "LEFT JOIN p.shares s " +
           "WHERE p.id = :plannerId " +
           "AND (p.user.id = :userId " +
           "OR p.visibility = 'PUBLIC' " +
           "OR s.sharedUser.id = :userId)")
    Optional<TravelPlanner> findAccessiblePlanner(@Param("plannerId") Long plannerId, @Param("userId") Long userId);

    // 공개 플래너 수
    long countByVisibility(TravelPlanner.Visibility visibility);
}
