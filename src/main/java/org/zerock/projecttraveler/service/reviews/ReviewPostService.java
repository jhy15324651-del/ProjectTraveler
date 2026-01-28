package org.zerock.projecttraveler.service.reviews;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerock.projecttraveler.dto.reviews.ReviewPostCreateRequest;
import org.zerock.projecttraveler.entity.reviews.ReviewPost;
import org.zerock.projecttraveler.repository.reviews.ReviewPostRepository;
import java.util.List;
import java.util.NoSuchElementException;


@Service
@RequiredArgsConstructor
public class ReviewPostService {

    private final ReviewPostRepository reviewPostRepository;

    /**
     * 여행 후기 저장
     */
    public Long create(ReviewPostCreateRequest request) {

        ReviewPost post = ReviewPost.builder()
                .writer("testUser") // 로그인 연동 전 임시
                .title(request.getTitle())
                .content(request.getContent())
                .travelType(request.getTravelType())
                .theme(request.getTheme())
                .period(request.getPeriod())
                .level(request.getLevel())
                .regionTags(request.getRegionTags())
                .budgetFlight(request.getBudgetFlight())
                .budgetLodging(request.getBudgetLodging())
                .budgetFood(request.getBudgetFood())
                .budgetExtra(request.getBudgetExtra())
                .build();

        ReviewPost saved = reviewPostRepository.save(post);
        return saved.getId();
    }

    public List<ReviewPost> listLatest() {
        return reviewPostRepository.findAllByOrderByCreatedAtDesc();
    }

    public ReviewPost findById(Long id) {
        return reviewPostRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ReviewPost not found: " + id));
    }
}
