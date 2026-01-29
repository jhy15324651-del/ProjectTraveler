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

    private void require(boolean cond, String msg) {
        if (!cond) throw new IllegalArgumentException(msg);
    }

    /**
     * 여행 후기 저장
     */
    public Long create(ReviewPostCreateRequest request) {

        require(request.getTravelType() != null && !request.getTravelType().isBlank(), "여행 유형을 선택해주세요.");
        require(request.getTheme() != null && !request.getTheme().isBlank(), "테마를 선택해주세요.");
        require(request.getPeriod() != null && !request.getPeriod().isBlank(), "기간을 선택해주세요.");
        require(request.getLevel() != null && !request.getLevel().isBlank(), "난이도를 선택해주세요.");
        require(request.getRegionTags() != null && !request.getRegionTags().isEmpty(), "지역을 1개 이상 선택해주세요.");

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
     * 최신순 목록 조회 (+ 썸네일/요약 추출)
     */
    public List<ReviewPost> listLatest() {
        List<ReviewPost> posts = reviewPostRepository.findAllByOrderByCreatedAtDesc();

        for (ReviewPost p : posts) {
            p.setThumbnailUrl(extractFirstImageUrl(p.getContent()));
            p.setSummary(extractTextOnly(p.getContent()));
        }

        return posts;
    }

    private String extractTextOnly(String html) {
        if (html == null || html.isBlank()) return "";
        String text = Jsoup.parse(html).text();
        text = text.replace("\u00A0", " ").trim(); // NBSP 처리
        return text.length() > 120 ? text.substring(0, 120) + "..." : text;
    }

    /**
     * 단건 조회 (+ 썸네일/요약 추출)
     */
    public ReviewPost findById(Long id) {
        ReviewPost post = reviewPostRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ReviewPost not found: " + id));

        post.setThumbnailUrl(extractFirstImageUrl(post.getContent()));
        post.setSummary(extractTextOnly(post.getContent()));
        return post;
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
