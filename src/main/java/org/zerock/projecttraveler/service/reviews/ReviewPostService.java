package org.zerock.projecttraveler.service.reviews;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

    /**
     * 최신순 목록 조회 (+ 썸네일 추출)
     */
    public List<ReviewPost> listLatest() {
        List<ReviewPost> posts = reviewPostRepository.findAllByOrderByCreatedAtDesc();

        for (ReviewPost p : posts) {
            // Quill HTML에서 첫 img src를 뽑아서 엔티티의 thumbnailUrl(@Transient)에 세팅
            p.setThumbnailUrl(extractFirstImageUrl(p.getContent()));

            // ✅ 요약(텍스트만)
            p.setSummary(extractTextOnly(p.getContent()));

        }

        return posts;
    }

    private String extractTextOnly(String html) {
        if (html == null || html.isBlank()) return "";
        return Jsoup.parse(html).text(); // ✅ HTML 태그, img src 같은 것 전부 제거됨
    }

    /**
     * 단건 조회
     */
    public ReviewPost findById(Long id) {
        return reviewPostRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ReviewPost not found: " + id));
    }

    /**
     * 본문(HTML)에서 첫 번째 이미지 src 추출
     */
    private String extractFirstImageUrl(String html) {
        if (html == null || html.isBlank()) return null;

        Document doc = Jsoup.parse(html);
        Element img = doc.selectFirst("img");
        if (img == null) return null;

        String src = img.attr("src");
        return (src == null || src.isBlank()) ? null : src;
    }


}
