package org.zerock.projecttraveler.repository.reviews;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.zerock.projecttraveler.entity.reviews.ReviewPost;

import java.util.List;

public class ReviewPostSpecs {

    // q: title/content like 검색
    public static Specification<ReviewPost> keyword(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return null;

            String like = "%" + q.trim() + "%";
            return cb.or(
                    cb.like(root.get("title"), like),
                    cb.like(root.get("content"), like)
            );
        };
    }

    // tags: regionTags OR (ElementCollection 대비: join + in)
    public static Specification<ReviewPost> regionTagsOr(List<String> tags) {
        return (root, query, cb) -> {
            if (tags == null || tags.isEmpty()) return null;

            query.distinct(true); // join 중복 방지
            Join<Object, Object> join = root.join("regionTags", JoinType.LEFT);
            return join.in(tags);
        };
    }

    // budgetTotal: min~max 범위
// budgetTotal: DB는 '만 원 단위'로 저장되어 있음
// 요청(min/maxBudget)은 '원 단위'로 들어오므로 /10000 변환 후 비교
    public static Specification<ReviewPost> budgetTotalBetween(Integer minWon, Integer maxWon) {
        return (root, query, cb) -> {
            if (minWon == null && maxWon == null) return null;

            // ✅ 원 -> 만원 변환
            // min은 올림(ceil) 처리해서 범위가 정확히 적용되게
            Integer minMan = null;
            if (minWon != null) {
                // ceil(minWon / 10000.0)
                minMan = (minWon + 9999) / 10000;
            }

            // max는 내림(floor) 처리
            Integer maxMan = null;
            if (maxWon != null) {
                maxMan = maxWon / 10000;
            }

            // max가 0~9999원처럼 너무 작으면 0이 될 수 있음 (정상)
            // minMan > maxMan 이 될 수도 있으니, 그 경우는 결과 없음 처리
            if (minMan != null && maxMan != null && minMan > maxMan) {
                // 항상 false 조건
                return cb.disjunction();
            }

            if (minMan != null && maxMan != null) {
                return cb.between(root.get("budgetTotal"), minMan, maxMan);
            } else if (minMan != null) {
                return cb.greaterThanOrEqualTo(root.get("budgetTotal"), minMan);
            } else {
                return cb.lessThanOrEqualTo(root.get("budgetTotal"), maxMan);
            }
        };
    }


    public static Specification<ReviewPost> travelTypeEq(String v) {
        return (root, query, cb) -> (v == null || v.isBlank()) ? null : cb.equal(root.get("travelType"), v);
    }

    public static Specification<ReviewPost> themeEq(String v) {
        return (root, query, cb) -> (v == null || v.isBlank()) ? null : cb.equal(root.get("theme"), v);
    }

    public static Specification<ReviewPost> periodIn(List<String> periods) {
        return (root, query, cb) ->
                (periods == null || periods.isEmpty()) ? null : root.get("period").in(periods);
    }

    public static Specification<ReviewPost> levelIn(List<String> levels) {
        return (root, query, cb) ->
                (levels == null || levels.isEmpty()) ? null : root.get("level").in(levels);
    }

    public static Specification<ReviewPost> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

}
