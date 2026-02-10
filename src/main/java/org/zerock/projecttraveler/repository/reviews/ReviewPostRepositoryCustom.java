package org.zerock.projecttraveler.repository.reviews;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.zerock.projecttraveler.dto.reviews.ReviewPostSearchRequest;
import org.zerock.projecttraveler.entity.reviews.ReviewPost;

public interface ReviewPostRepositoryCustom {
    Page<ReviewPost> searchWithRegionPriority(ReviewPostSearchRequest req, Pageable pageable);
}
