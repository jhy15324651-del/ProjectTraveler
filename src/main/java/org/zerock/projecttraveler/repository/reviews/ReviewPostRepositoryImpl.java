package org.zerock.projecttraveler.repository.reviews;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.zerock.projecttraveler.dto.reviews.ReviewPostSearchRequest;
import org.zerock.projecttraveler.entity.reviews.ReviewPost;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class ReviewPostRepositoryImpl implements ReviewPostRepositoryCustom {

    @PersistenceContext
    private final EntityManager em;

    @Override
    public Page<ReviewPost> searchWithRegionPriority(ReviewPostSearchRequest req, Pageable pageable) {

        StringBuilder sql = new StringBuilder();
        StringBuilder countSql = new StringBuilder();

        sql.append("""
        SELECT p.*
        FROM review_post p
    """);

        countSql.append("""
        SELECT COUNT(DISTINCT p.id)
        FROM review_post p
    """);

        Map<String, Object> params = new HashMap<>();

        boolean hasTags = req.getTags() != null && !req.getTags().isEmpty();
        boolean hasThemes = req.getThemes() != null && !req.getThemes().isEmpty();

        // ✅ 필요한 조인만 건다
        if (hasTags) {
            sql.append(" JOIN review_post_region_tag t ON p.id = t.post_id ");
            countSql.append(" JOIN review_post_region_tag t ON p.id = t.post_id ");
        }
        if (hasThemes) {
            sql.append(" JOIN review_post_theme th ON p.id = th.post_id ");
            countSql.append(" JOIN review_post_theme th ON p.id = th.post_id ");
        }

        sql.append(" WHERE 1=1 ");
        countSql.append(" WHERE 1=1 ");

        // ✅ 소프트삭제 제외
        sql.append(" AND COALESCE(p.deleted, 0) = 0 ");
        countSql.append(" AND COALESCE(p.deleted, 0) = 0 ");

        // (1) 키워드 q : 제목/내용
        if (req.getQ() != null && !req.getQ().trim().isEmpty()) {
            sql.append(" AND (p.title LIKE :kw OR p.content LIKE :kw) ");
            countSql.append(" AND (p.title LIKE :kw OR p.content LIKE :kw) ");
            params.put("kw", "%" + req.getQ().trim() + "%");
        }

        // (2) 단일 선택 travelType
        if (req.getTravelType() != null && !req.getTravelType().isBlank()) {
            sql.append(" AND p.travel_type = :travelType ");
            countSql.append(" AND p.travel_type = :travelType ");
            params.put("travelType", req.getTravelType());
        }

        // ✅ (2-1) 다중 선택 themes
        if (hasThemes) {
            sql.append(" AND th.theme IN (:themes) ");
            countSql.append(" AND th.theme IN (:themes) ");
            params.put("themes", req.getThemes());
        }

        // (3) 기간/난이도 다중 선택 (OR)
        if (req.getPeriods() != null && !req.getPeriods().isEmpty()) {
            sql.append(" AND p.period IN (:periods) ");
            countSql.append(" AND p.period IN (:periods) ");
            params.put("periods", req.getPeriods());
        }

        if (req.getLevels() != null && !req.getLevels().isEmpty()) {
            sql.append(" AND p.level IN (:levels) ");
            countSql.append(" AND p.level IN (:levels) ");
            params.put("levels", req.getLevels());
        }

        // (4) 예산 범위
        if (req.getMinBudget() != null) {
            sql.append(" AND p.budget_total >= :minBudget ");
            countSql.append(" AND p.budget_total >= :minBudget ");
            params.put("minBudget", req.getMinBudget());
        }
        if (req.getMaxBudget() != null) {
            sql.append(" AND p.budget_total <= :maxBudget ");
            countSql.append(" AND p.budget_total <= :maxBudget ");
            params.put("maxBudget", req.getMaxBudget());
        }

        // (5) 지역 태그(OR 후보군)
        if (hasTags) {
            sql.append(" AND t.tag IN (:tags) ");
            countSql.append(" AND t.tag IN (:tags) ");
            params.put("tags", req.getTags());
        }

        // ✅ 중복 row 제거
        // tags/themes 둘 중 하나라도 join이면 GROUP BY가 안전함
        if (hasTags || hasThemes) {
            sql.append(" GROUP BY p.id ");
        }

        // ===== 정렬 =====
        if (hasTags) {
            // 지역 선택이 있으면 지역 일치 개수 우선
            sql.append(" ORDER BY COUNT(DISTINCT t.tag) DESC, p.created_at DESC ");
        } else {
            sql.append(" ORDER BY p.created_at DESC ");
        }

        Query q = em.createNativeQuery(sql.toString(), ReviewPost.class);
        Query cq = em.createNativeQuery(countSql.toString());

        params.forEach((k, v) -> {
            q.setParameter(k, v);
            cq.setParameter(k, v);
        });

        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<ReviewPost> content = q.getResultList();

        long total = ((Number) cq.getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }
}
