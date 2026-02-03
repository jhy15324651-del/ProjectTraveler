package org.zerock.projecttraveler.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.entity.*;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.PlannerService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planner")
@RequiredArgsConstructor
public class PlannerApiController {

    private final PlannerService plannerService;

    // ==================== 플래너 CRUD ====================

    /**
     * 플래너 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPlanner(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        if (!plannerService.canAccess(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "접근 권한이 없습니다."));
        }

        return plannerService.findByIdWithUser(id)
                .map(planner -> {
                    // 조회수 증가 (본인이 아닌 경우)
                    if (!planner.getUser().getId().equals(userId)) {
                        plannerService.incrementViewCount(id);
                    }

                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("id", planner.getId());
                    result.put("title", planner.getTitle());
                    result.put("destination", planner.getDestination() != null ? planner.getDestination() : "");
                    result.put("description", planner.getDescription() != null ? planner.getDescription() : "");
                    result.put("startDate", planner.getStartDate() != null ? planner.getStartDate().toString() : "");
                    result.put("endDate", planner.getEndDate() != null ? planner.getEndDate().toString() : "");
                    result.put("days", planner.getDays());
                    result.put("visibility", planner.getVisibility().name());
                    result.put("coverImage", planner.getCoverImage() != null ? planner.getCoverImage() : "");
                    result.put("viewCount", planner.getViewCount());
                    result.put("likeCount", planner.getLikeCount());
                    result.put("authorName", planner.getAuthorName());
                    result.put("isOwner", planner.getUser().getId().equals(userId));
                    result.put("canEdit", plannerService.canEdit(id, userId));

                    // 일정 목록
                    List<PlannerItinerary> itineraries = plannerService.getItineraries(id);
                    result.put("itineraries", itineraries.stream().map(it -> Map.of(
                            "id", it.getId(),
                            "dayIndex", it.getDayIndex(),
                            "sortOrder", it.getSortOrder(),
                            "time", it.getTime() != null ? it.getTime() : "",
                            "title", it.getTitle(),
                            "location", it.getLocation() != null ? it.getLocation() : "",
                            "category", it.getCategory().name(),
                            "notes", it.getNotes() != null ? it.getNotes() : "",
                            "cost", it.getCost(),
                            "completed", it.getCompleted()
                    )).toList());

                    // 체크리스트
                    List<PlannerChecklist> checklists = plannerService.getChecklists(id);
                    result.put("checklists", checklists.stream().map(cl -> Map.of(
                            "id", cl.getId(),
                            "category", cl.getCategory() != null ? cl.getCategory() : "",
                            "text", cl.getText(),
                            "completed", cl.getCompleted()
                    )).toList());

                    // 예산
                    List<PlannerBudget> budgets = plannerService.getBudgets(id);
                    result.put("budgets", budgets.stream().map(b -> Map.of(
                            "id", b.getId(),
                            "name", b.getName(),
                            "plannedAmount", b.getPlannedAmount(),
                            "actualAmount", b.getActualAmount()
                    )).toList());

                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 플래너 생성
     */
    @PostMapping
    public ResponseEntity<?> createPlanner(@RequestBody Map<String, Object> request) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        String title = (String) request.get("title");
        String destination = (String) request.get("destination");
        LocalDate startDate = LocalDate.parse((String) request.get("startDate"));
        LocalDate endDate = LocalDate.parse((String) request.get("endDate"));
        String templateStr = (String) request.getOrDefault("template", "BLANK");
        TravelPlanner.Template template = TravelPlanner.Template.valueOf(templateStr.toUpperCase());

        TravelPlanner planner = plannerService.createPlanner(userId, title, destination, startDate, endDate, template);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "plannerId", planner.getId()
        ));
    }

    /**
     * 플래너 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlanner(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        if (!plannerService.canEdit(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "편집 권한이 없습니다."));
        }

        String title = (String) request.get("title");
        String destination = (String) request.get("destination");
        String description = (String) request.get("description");
        LocalDate startDate = request.get("startDate") != null ? LocalDate.parse((String) request.get("startDate")) : null;
        LocalDate endDate = request.get("endDate") != null ? LocalDate.parse((String) request.get("endDate")) : null;
        String coverImage = (String) request.get("coverImage");

        plannerService.updatePlanner(id, title, destination, description, startDate, endDate, coverImage);

        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 플래너 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlanner(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        if (!plannerService.canEdit(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "삭제 권한이 없습니다."));
        }

        plannerService.deletePlanner(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 내 플래너 목록
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyPlanners() {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<TravelPlanner> planners = plannerService.getMyPlanners(userId);

        List<Map<String, Object>> result = planners.stream().map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "title", p.getTitle(),
                "destination", p.getDestination() != null ? p.getDestination() : "",
                "startDate", p.getStartDate() != null ? p.getStartDate().toString() : "",
                "endDate", p.getEndDate() != null ? p.getEndDate().toString() : "",
                "days", p.getDays(),
                "visibility", p.getVisibility().name(),
                "coverImage", p.getCoverImage() != null ? p.getCoverImage() : "",
                "viewCount", p.getViewCount(),
                "likeCount", p.getLikeCount()
        )).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * 공개 플래너 목록
     */
    @GetMapping("/public")
    public ResponseEntity<?> getPublicPlanners(
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String keyword) {

        List<TravelPlanner> planners;
        if (keyword != null && !keyword.isBlank()) {
            planners = plannerService.searchPlanners(keyword);
        } else if ("popular".equals(sort)) {
            planners = plannerService.getPopularPlanners();
        } else {
            planners = plannerService.getPublicPlanners();
        }

        List<Map<String, Object>> result = planners.stream().map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "title", p.getTitle(),
                "destination", p.getDestination() != null ? p.getDestination() : "",
                "startDate", p.getStartDate() != null ? p.getStartDate().toString() : "",
                "endDate", p.getEndDate() != null ? p.getEndDate().toString() : "",
                "days", p.getDays(),
                "authorName", p.getAuthorName(),
                "coverImage", p.getCoverImage() != null ? p.getCoverImage() : "",
                "viewCount", p.getViewCount(),
                "likeCount", p.getLikeCount()
        )).toList();

        return ResponseEntity.ok(result);
    }

    // ==================== 공개 설정 ====================

    /**
     * 공개 설정 변경
     */
    @PutMapping("/{id}/visibility")
    public ResponseEntity<?> updateVisibility(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        if (!plannerService.canEdit(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }

        String visibilityStr = request.get("visibility");
        TravelPlanner.Visibility visibility = TravelPlanner.Visibility.valueOf(visibilityStr.toUpperCase());

        plannerService.updateVisibility(id, visibility);

        return ResponseEntity.ok(Map.of("success", true));
    }

    // ==================== 공유 관리 ====================

    /**
     * 플래너 공유
     */
    @PostMapping("/{id}/share")
    public ResponseEntity<?> sharePlanner(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        if (!plannerService.canEdit(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }

        Long sharedUserId = ((Number) request.get("userId")).longValue();
        String permissionStr = (String) request.getOrDefault("permission", "VIEW");
        PlannerShare.Permission permission = PlannerShare.Permission.valueOf(permissionStr.toUpperCase());

        plannerService.sharePlanner(id, sharedUserId, permission);

        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 공유 취소
     */
    @DeleteMapping("/{id}/share/{sharedUserId}")
    public ResponseEntity<?> unsharePlanner(@PathVariable Long id, @PathVariable Long sharedUserId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        if (!plannerService.canEdit(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }

        plannerService.unsharePlanner(id, sharedUserId);

        return ResponseEntity.ok(Map.of("success", true));
    }

    // ==================== 일정 관리 ====================

    /**
     * 일정 추가
     */
    @PostMapping("/{id}/itinerary")
    public ResponseEntity<?> addItinerary(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        if (!plannerService.canEdit(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }

        Integer dayIndex = ((Number) request.get("dayIndex")).intValue();
        String time = (String) request.get("time");
        String title = (String) request.get("title");
        String location = (String) request.get("location");
        String categoryStr = (String) request.getOrDefault("category", "OTHER");
        PlannerItinerary.Category category = PlannerItinerary.Category.valueOf(categoryStr.toUpperCase());
        String notes = (String) request.get("notes");
        Integer cost = request.get("cost") != null ? ((Number) request.get("cost")).intValue() : 0;

        PlannerItinerary itinerary = plannerService.addItinerary(id, dayIndex, time, title, location, category, notes, cost);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "id", itinerary.getId()
        ));
    }

    /**
     * 일정 수정
     */
    @PutMapping("/itinerary/{itineraryId}")
    public ResponseEntity<?> updateItinerary(@PathVariable Long itineraryId, @RequestBody Map<String, Object> request) {
        String time = (String) request.get("time");
        String title = (String) request.get("title");
        String location = (String) request.get("location");
        String categoryStr = (String) request.get("category");
        PlannerItinerary.Category category = categoryStr != null ? PlannerItinerary.Category.valueOf(categoryStr.toUpperCase()) : null;
        String notes = (String) request.get("notes");
        Integer cost = request.get("cost") != null ? ((Number) request.get("cost")).intValue() : null;
        Boolean completed = (Boolean) request.get("completed");

        plannerService.updateItinerary(itineraryId, time, title, location, category, notes, cost, completed);

        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 일정 삭제
     */
    @DeleteMapping("/itinerary/{itineraryId}")
    public ResponseEntity<?> deleteItinerary(@PathVariable Long itineraryId) {
        plannerService.deleteItinerary(itineraryId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ==================== 체크리스트 관리 ====================

    /**
     * 체크리스트 항목 추가
     */
    @PostMapping("/{id}/checklist")
    public ResponseEntity<?> addChecklistItem(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        if (!plannerService.canEdit(id, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }

        String category = request.getOrDefault("category", "기타");
        String text = request.get("text");

        PlannerChecklist item = plannerService.addChecklistItem(id, category, text);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "id", item.getId()
        ));
    }

    /**
     * 체크리스트 완료 토글
     */
    @PutMapping("/checklist/{itemId}/toggle")
    public ResponseEntity<?> toggleChecklistItem(@PathVariable Long itemId) {
        plannerService.toggleChecklistItem(itemId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 체크리스트 항목 삭제
     */
    @DeleteMapping("/checklist/{itemId}")
    public ResponseEntity<?> deleteChecklistItem(@PathVariable Long itemId) {
        plannerService.deleteChecklistItem(itemId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ==================== 예산 관리 ====================

    /**
     * 예산 실제 지출 업데이트
     */
    @PutMapping("/budget/{budgetId}")
    public ResponseEntity<?> updateBudgetActual(@PathVariable Long budgetId, @RequestBody Map<String, Object> request) {
        Integer actualAmount = ((Number) request.get("actualAmount")).intValue();
        plannerService.updateBudgetActual(budgetId, actualAmount);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
