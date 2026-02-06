package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.PlannerItinerary;

import java.util.List;

public interface PlannerItineraryRepository extends JpaRepository<PlannerItinerary, Long> {

    List<PlannerItinerary> findByPlannerIdOrderByDayIndexAscSortOrderAsc(Long plannerId);

    List<PlannerItinerary> findByPlannerIdAndDayIndexOrderBySortOrderAsc(Long plannerId, Integer dayIndex);

    void deleteByPlannerId(Long plannerId);

    /**
     * 플래너별 카테고리별 예상비용 합계 조회
     */
    @Query("SELECT i.category, COALESCE(SUM(i.cost), 0) " +
           "FROM PlannerItinerary i " +
           "WHERE i.planner.id = :plannerId " +
           "GROUP BY i.category")
    List<Object[]> sumCostByCategory(@Param("plannerId") Long plannerId);
}
