package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.projecttraveler.entity.PlannerItinerary;

import java.util.List;

public interface PlannerItineraryRepository extends JpaRepository<PlannerItinerary, Long> {

    List<PlannerItinerary> findByPlannerIdOrderByDayIndexAscSortOrderAsc(Long plannerId);

    List<PlannerItinerary> findByPlannerIdAndDayIndexOrderBySortOrderAsc(Long plannerId, Integer dayIndex);

    void deleteByPlannerId(Long plannerId);
}
