package org.zerock.projecttraveler.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.projecttraveler.entity.*;
import org.zerock.projecttraveler.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlannerService {

    private final TravelPlannerRepository plannerRepository;
    private final PlannerItineraryRepository itineraryRepository;
    private final PlannerChecklistRepository checklistRepository;
    private final PlannerBudgetRepository budgetRepository;
    private final PlannerShareRepository shareRepository;
    private final UserRepository userRepository;

    // ==================== 플래너 CRUD ====================

    /**
     * 새 플래너 생성
     */
    @Transactional
    public TravelPlanner createPlanner(Long userId, String title, String destination,
                                       LocalDate startDate, LocalDate endDate,
                                       TravelPlanner.Template template) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        TravelPlanner planner = TravelPlanner.builder()
                .user(user)
                .title(title)
                .destination(destination)
                .startDate(startDate)
                .endDate(endDate)
                .template(template)
                .visibility(TravelPlanner.Visibility.PRIVATE)
                .build();

        return plannerRepository.save(planner);
    }

    /**
     * 플래너 조회
     */
    public Optional<TravelPlanner> findById(Long id) {
        return plannerRepository.findById(id);
    }

    /**
     * 플래너 상세 조회 (사용자 정보 포함)
     */
    public Optional<TravelPlanner> findByIdWithUser(Long id) {
        return plannerRepository.findByIdWithUser(id);
    }

    /**
     * 내 플래너 목록
     */
    public List<TravelPlanner> getMyPlanners(Long userId) {
        return plannerRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 공개된 플래너 목록 (최신순)
     */
    public List<TravelPlanner> getPublicPlanners() {
        return plannerRepository.findByVisibilityOrderByCreatedAtDesc(TravelPlanner.Visibility.PUBLIC);
    }

    /**
     * 공개된 플래너 목록 (페이징)
     */
    public Page<TravelPlanner> getPublicPlanners(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return plannerRepository.findByVisibilityOrderByCreatedAtDesc(TravelPlanner.Visibility.PUBLIC, pageable);
    }

    /**
     * 공개된 플래너 목록 (인기순)
     */
    public List<TravelPlanner> getPopularPlanners() {
        return plannerRepository.findByVisibilityOrderByViewCountDesc(TravelPlanner.Visibility.PUBLIC);
    }

    /**
     * 플래너 검색
     */
    public List<TravelPlanner> searchPlanners(String keyword) {
        return plannerRepository.searchPublicPlanners(keyword);
    }

    /**
     * 플래너 수정
     */
    @Transactional
    public TravelPlanner updatePlanner(Long plannerId, String title, String destination,
                                       String description, LocalDate startDate, LocalDate endDate,
                                       String coverImage) {
        TravelPlanner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new IllegalArgumentException("플래너를 찾을 수 없습니다."));

        planner.setTitle(title);
        planner.setDestination(destination);
        planner.setDescription(description);
        planner.setStartDate(startDate);
        planner.setEndDate(endDate);
        planner.setCoverImage(coverImage);

        return planner;
    }

    /**
     * 플래너 삭제
     */
    @Transactional
    public void deletePlanner(Long plannerId) {
        plannerRepository.deleteById(plannerId);
    }

    /**
     * 조회수 증가
     */
    @Transactional
    public void incrementViewCount(Long plannerId) {
        plannerRepository.findById(plannerId).ifPresent(planner -> {
            planner.setViewCount(planner.getViewCount() + 1);
        });
    }

    // ==================== 공개 설정 ====================

    /**
     * 공개 설정 변경
     */
    @Transactional
    public void updateVisibility(Long plannerId, TravelPlanner.Visibility visibility) {
        TravelPlanner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new IllegalArgumentException("플래너를 찾을 수 없습니다."));
        planner.setVisibility(visibility);
    }

    /**
     * 접근 권한 확인
     */
    public boolean canAccess(Long plannerId, Long userId) {
        Optional<TravelPlanner> planner = plannerRepository.findById(plannerId);
        if (planner.isEmpty()) return false;

        TravelPlanner p = planner.get();

        // 본인 플래너
        if (p.getUser().getId().equals(userId)) return true;

        // 공개 플래너
        if (p.isPublic()) return true;

        // 공유받은 플래너
        return shareRepository.existsByPlannerIdAndSharedUserId(plannerId, userId);
    }

    /**
     * 편집 권한 확인
     */
    public boolean canEdit(Long plannerId, Long userId) {
        Optional<TravelPlanner> planner = plannerRepository.findById(plannerId);
        if (planner.isEmpty()) return false;

        TravelPlanner p = planner.get();

        // 본인 플래너
        if (p.getUser().getId().equals(userId)) return true;

        // 편집 권한으로 공유받은 플래너
        Optional<PlannerShare> share = shareRepository.findByPlannerIdAndSharedUserId(plannerId, userId);
        return share.isPresent() && share.get().getPermission() == PlannerShare.Permission.EDIT;
    }

    // ==================== 공유 관리 ====================

    /**
     * 플래너 공유
     */
    @Transactional
    public PlannerShare sharePlanner(Long plannerId, Long sharedUserId, PlannerShare.Permission permission) {
        TravelPlanner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new IllegalArgumentException("플래너를 찾을 수 없습니다."));

        User sharedUser = userRepository.findById(sharedUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 이미 공유되어 있으면 권한 업데이트
        Optional<PlannerShare> existingShare = shareRepository.findByPlannerIdAndSharedUserId(plannerId, sharedUserId);
        if (existingShare.isPresent()) {
            existingShare.get().setPermission(permission);
            return existingShare.get();
        }

        PlannerShare share = PlannerShare.builder()
                .planner(planner)
                .sharedUser(sharedUser)
                .permission(permission)
                .build();

        return shareRepository.save(share);
    }

    /**
     * 공유 취소
     */
    @Transactional
    public void unsharePlanner(Long plannerId, Long sharedUserId) {
        shareRepository.findByPlannerIdAndSharedUserId(plannerId, sharedUserId)
                .ifPresent(shareRepository::delete);
    }

    /**
     * 플래너 공유 목록
     */
    public List<PlannerShare> getShares(Long plannerId) {
        return shareRepository.findByPlannerId(plannerId);
    }

    /**
     * 나에게 공유된 플래너 목록
     */
    public List<PlannerShare> getSharedWithMe(Long userId) {
        return shareRepository.findBySharedUserId(userId);
    }

    // ==================== 일정 관리 ====================

    /**
     * 일정 목록 조회
     */
    public List<PlannerItinerary> getItineraries(Long plannerId) {
        return itineraryRepository.findByPlannerIdOrderByDayIndexAscSortOrderAsc(plannerId);
    }

    /**
     * 특정 일차 일정 조회
     */
    public List<PlannerItinerary> getItinerariesByDay(Long plannerId, Integer dayIndex) {
        return itineraryRepository.findByPlannerIdAndDayIndexOrderBySortOrderAsc(plannerId, dayIndex);
    }

    /**
     * 일정 추가
     */
    @Transactional
    public PlannerItinerary addItinerary(Long plannerId, Integer dayIndex, String time,
                                         String title, String location,
                                         PlannerItinerary.Category category,
                                         String notes, Integer cost) {
        TravelPlanner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new IllegalArgumentException("플래너를 찾을 수 없습니다."));

        List<PlannerItinerary> existing = itineraryRepository.findByPlannerIdAndDayIndexOrderBySortOrderAsc(plannerId, dayIndex);
        int nextOrder = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getSortOrder() + 1;

        PlannerItinerary itinerary = PlannerItinerary.builder()
                .planner(planner)
                .dayIndex(dayIndex)
                .sortOrder(nextOrder)
                .time(time)
                .title(title)
                .location(location)
                .category(category)
                .notes(notes)
                .cost(cost != null ? cost : 0)
                .build();

        return itineraryRepository.save(itinerary);
    }

    /**
     * 일정 수정
     */
    @Transactional
    public PlannerItinerary updateItinerary(Long itineraryId, String time, String title,
                                            String location, PlannerItinerary.Category category,
                                            String notes, Integer cost, Boolean completed) {
        PlannerItinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        if (time != null) itinerary.setTime(time);
        if (title != null) itinerary.setTitle(title);
        if (location != null) itinerary.setLocation(location);
        if (category != null) itinerary.setCategory(category);
        if (notes != null) itinerary.setNotes(notes);
        if (cost != null) itinerary.setCost(cost);
        if (completed != null) itinerary.setCompleted(completed);

        return itinerary;
    }

    /**
     * 일정 삭제
     */
    @Transactional
    public void deleteItinerary(Long itineraryId) {
        itineraryRepository.deleteById(itineraryId);
    }

    // ==================== 체크리스트 관리 ====================

    /**
     * 체크리스트 조회
     */
    public List<PlannerChecklist> getChecklists(Long plannerId) {
        return checklistRepository.findByPlannerIdOrderBySortOrderAsc(plannerId);
    }

    /**
     * 체크리스트 항목 추가
     */
    @Transactional
    public PlannerChecklist addChecklistItem(Long plannerId, String category, String text) {
        TravelPlanner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new IllegalArgumentException("플래너를 찾을 수 없습니다."));

        List<PlannerChecklist> existing = checklistRepository.findByPlannerIdOrderBySortOrderAsc(plannerId);
        int nextOrder = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getSortOrder() + 1;

        PlannerChecklist item = PlannerChecklist.builder()
                .planner(planner)
                .sortOrder(nextOrder)
                .category(category)
                .text(text)
                .build();

        return checklistRepository.save(item);
    }

    /**
     * 체크리스트 완료 토글
     */
    @Transactional
    public void toggleChecklistItem(Long itemId) {
        checklistRepository.findById(itemId).ifPresent(item -> {
            item.setCompleted(!item.getCompleted());
        });
    }

    /**
     * 체크리스트 항목 삭제
     */
    @Transactional
    public void deleteChecklistItem(Long itemId) {
        checklistRepository.deleteById(itemId);
    }

    // ==================== 예산 관리 ====================

    /**
     * 예산 목록 조회
     */
    public List<PlannerBudget> getBudgets(Long plannerId) {
        return budgetRepository.findByPlannerIdOrderBySortOrderAsc(plannerId);
    }

    /**
     * 예산 항목 추가
     */
    @Transactional
    public PlannerBudget addBudgetItem(Long plannerId, String name, Integer plannedAmount) {
        TravelPlanner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new IllegalArgumentException("플래너를 찾을 수 없습니다."));

        List<PlannerBudget> existing = budgetRepository.findByPlannerIdOrderBySortOrderAsc(plannerId);
        int nextOrder = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getSortOrder() + 1;

        PlannerBudget budget = PlannerBudget.builder()
                .planner(planner)
                .sortOrder(nextOrder)
                .name(name)
                .plannedAmount(plannedAmount != null ? plannedAmount : 0)
                .build();

        return budgetRepository.save(budget);
    }

    /**
     * 예산 실제 지출 업데이트
     */
    @Transactional
    public void updateBudgetActual(Long budgetId, Integer actualAmount) {
        budgetRepository.findById(budgetId).ifPresent(budget -> {
            budget.setActualAmount(actualAmount);
        });
    }

    /**
     * 예산 항목 삭제
     */
    @Transactional
    public void deleteBudgetItem(Long budgetId) {
        budgetRepository.deleteById(budgetId);
    }
}
