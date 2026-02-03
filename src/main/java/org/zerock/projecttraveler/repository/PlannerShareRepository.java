package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.projecttraveler.entity.PlannerShare;

import java.util.List;
import java.util.Optional;

public interface PlannerShareRepository extends JpaRepository<PlannerShare, Long> {

    List<PlannerShare> findByPlannerId(Long plannerId);

    List<PlannerShare> findBySharedUserId(Long userId);

    Optional<PlannerShare> findByPlannerIdAndSharedUserId(Long plannerId, Long userId);

    boolean existsByPlannerIdAndSharedUserId(Long plannerId, Long userId);

    void deleteByPlannerId(Long plannerId);
}
