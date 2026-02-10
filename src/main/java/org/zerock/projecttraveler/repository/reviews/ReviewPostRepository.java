package org.zerock.projecttraveler.repository.reviews;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.reviews.ReviewPost;

import java.util.List;

public interface ReviewPostRepository
        extends JpaRepository<ReviewPost, Long>,
        JpaSpecificationExecutor<ReviewPost>,
        ReviewPostRepositoryCustom {

    // 최신순 목록
    List<ReviewPost> findAllByOrderByCreatedAtDesc();


    @Query(
            value = """
        SELECT p.*
        FROM review_post p
        JOIN review_post_region_tag t ON p.id = t.post_id
        WHERE t.tag IN (:tags)
        GROUP BY p.id
        ORDER BY COUNT(DISTINCT t.tag) DESC, p.created_at DESC
      """,
            countQuery = """
        SELECT COUNT(DISTINCT p.id)
        FROM review_post p
        JOIN review_post_region_tag t ON p.id = t.post_id
        WHERE t.tag IN (:tags)
      """,
            nativeQuery = true
    )
    Page<ReviewPost> findByRegionTagsOrderByMatchCount(@Param("tags") List<String> tags, Pageable pageable);
}


