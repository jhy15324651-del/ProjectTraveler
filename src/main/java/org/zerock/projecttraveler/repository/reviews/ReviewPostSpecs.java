package org.zerock.projecttraveler.repository.reviews;

import org.springframework.data.jpa.domain.Specification;
import org.zerock.projecttraveler.entity.reviews.ReviewPost;

import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewPostSpecs {

    // 1) 제목/내용 키워드 검색
    public static Specification<ReviewPost> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();

            String like = "%" + keyword.trim() + "%";
            return cb.or(
                    cb.like(root.get("title"), like),
                    cb.like(root.get("content"), like)
            );
        };
    }

    // 2) 지역태그 OR 검색 (regionTags 중 하나라도 포함되면 OK)
    public static Specification<ReviewPost> regionTagsOr(List<String> tags) {
        return (root, query, cb) -> {
            if (tags == null || tags.isEmpty()) return cb.conjunction();

            // 중복 row 방지
            query.distinct(true);

            // regionTags는 ElementCollection이라 join 필요
            Join<ReviewPost, String> tagJoin = root.join("regionTags", JoinType.INNER);

            // OR 조건: tag in (tags) 형태가 가장 깔끔
            CriteriaBuilder.In<String> inClause = cb.in(tagJoin);
            for (String t : tags) {
                if (t == null || t.isBlank()) continue;
                inClause.value(t.trim());
            }

            return inClause;
        };
    }

    // 3) 예산 총합 범위 검색 (budgetTotal)
    public static Specification<ReviewPost> budgetTotalBetween(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return cb.conjunction();

            Path<Integer> p = root.get("budgetTotal");

            if (min != null && max != null) return cb.between(p, min, max);
            if (min != null) return cb.greaterThanOrEqualTo(p, min);
            return cb.lessThanOrEqualTo(p, max);
        };
    }
}
