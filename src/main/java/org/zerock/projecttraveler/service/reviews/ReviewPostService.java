package org.zerock.projecttraveler.service.reviews;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.zerock.projecttraveler.dto.reviews.ReviewPostCreateRequest;
import org.zerock.projecttraveler.dto.reviews.ReviewPostSearchRequest;
import org.zerock.projecttraveler.entity.User;
import org.zerock.projecttraveler.entity.reviews.ReviewPost;
import org.zerock.projecttraveler.repository.UserRepository;
import org.zerock.projecttraveler.repository.reviews.ReviewPostRepository;
import org.zerock.projecttraveler.repository.reviews.ReviewPostSpecs;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ReviewPostService {

    private final ReviewPostRepository reviewPostRepository;
    private final UserRepository userRepository;

    private void require(boolean cond, String msg) {
        if (!cond) throw new IllegalArgumentException(msg);
    }

    /**
     * 여행 후기 저장
     * - writer: 로그인 사용자(User) 연관관계로 저장
     */
    public Long create(ReviewPostCreateRequest request) {

        require(request.getTravelType() != null && !request.getTravelType().isBlank(), "여행 유형을 선택해주세요.");
        require(request.getTheme() != null && !request.getTheme().isBlank(), "테마를 선택해주세요.");
        require(request.getPeriod() != null && !request.getPeriod().isBlank(), "기간을 선택해주세요.");
        require(request.getLevel() != null && !request.getLevel().isBlank(), "난이도를 선택해주세요.");
        require(request.getRegionTags() != null && !request.getRegionTags().isEmpty(), "지역을 1개 이상 선택해주세요.");

        // ✅ 로그인 principal에서 username 꺼내기
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // ✅ users 테이블에서 사용자 조회
        User writer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자 정보를 찾을 수 없습니다: " + username));

        ReviewPost post = ReviewPost.builder()
                .writer(writer)
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
        posts.forEach(this::fillThumbAndSummary);
        return posts;
    }

    /**
     * 검색 + 페이징 (+ 썸네일/요약 추출)
     * - q: 제목/본문 키워드
     * - tags: regionTags OR
     * - minBudget/maxBudget: budgetTotal 범위
     */
    public Page<ReviewPost> search(ReviewPostSearchRequest req) {

        int page = (req.getPage() == null || req.getPage() < 0) ? 0 : req.getPage();
        int size = (req.getSize() == null || req.getSize() <= 0) ? 12 : req.getSize();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<ReviewPost> spec =
                Specification.where(ReviewPostSpecs.keyword(req.getQ()))
                        .and(ReviewPostSpecs.regionTagsOr(req.getTags()))
                        .and(ReviewPostSpecs.budgetTotalBetween(req.getMinBudget(), req.getMaxBudget()));

        Page<ReviewPost> result = reviewPostRepository.findAll(spec, pageable);

        // ✅ 검색 결과에도 카드 썸네일/요약 적용
        result.getContent().forEach(this::fillThumbAndSummary);

        return result;
    }

    /**
     * 단건 조회 (+ 썸네일/요약 추출)
     */
    public ReviewPost findById(Long id) {
        ReviewPost post = reviewPostRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ReviewPost not found: " + id));

        fillThumbAndSummary(post);
        return post;
    }

    /**
     * 공통: 썸네일/요약 채우기
     */
    private void fillThumbAndSummary(ReviewPost p) {
        p.setThumbnailUrl(extractFirstImageUrl(p.getContent()));
        p.setSummary(extractTextOnly(p.getContent()));
    }

    /**
     * HTML에서 텍스트만 추출 후 120자 요약
     */
    private String extractTextOnly(String html) {
        if (html == null || html.isBlank()) return "";
        String text = Jsoup.parse(html).text();
        text = text.replace("\u00A0", " ").trim(); // NBSP 처리
        return text.length() > 120 ? text.substring(0, 120) + "..." : text;
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
