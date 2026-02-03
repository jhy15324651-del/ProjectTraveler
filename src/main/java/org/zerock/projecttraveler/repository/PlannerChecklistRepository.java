package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.projecttraveler.entity.PlannerChecklist;

import java.util.List;

public interface PlannerChecklistRepository extends JpaRepository<PlannerChecklist, Long> {

    List<PlannerChecklist> findByPlannerIdOrderBySortOrderAsc(Long plannerId);

    void deleteByPlannerId(Long plannerId);
}
