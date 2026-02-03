package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.projecttraveler.entity.PlannerBudget;

import java.util.List;

public interface PlannerBudgetRepository extends JpaRepository<PlannerBudget, Long> {

    List<PlannerBudget> findByPlannerIdOrderBySortOrderAsc(Long plannerId);

    void deleteByPlannerId(Long plannerId);
}
