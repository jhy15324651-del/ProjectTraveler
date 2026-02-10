package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.PlannerShare;

import java.util.List;
import java.util.Optional;

public interface PlannerShareRepository extends JpaRepository<PlannerShare, Long> {

    List<PlannerShare> findByPlannerId(Long plannerId);

    List<PlannerShare> findBySharedUserId(Long userId);

    /**
     * 공유받은 플래너 목록 조회 (플래너와 작성자 정보 포함)
     */
    @Query("SELECT s FROM PlannerShare s " +
           "JOIN FETCH s.planner p " +
           "JOIN FETCH p.user " +
           "WHERE s.sharedUser.id = :userId")
    List<PlannerShare> findBySharedUserIdWithPlanner(@Param("userId") Long userId);

    Optional<PlannerShare> findByPlannerIdAndSharedUserId(Long plannerId, Long userId);

    boolean existsByPlannerIdAndSharedUserId(Long plannerId, Long userId);

    void deleteByPlannerId(Long plannerId);
}
