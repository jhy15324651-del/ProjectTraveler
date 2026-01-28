package org.zerock.projecttraveler.repository.reviews;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.projecttraveler.entity.reviews.ReviewPost;

import java.util.List;

public interface ReviewPostRepository extends JpaRepository<ReviewPost, Long> {

    // 최신순 목록
    List<ReviewPost> findAllByOrderByCreatedAtDesc();
}
