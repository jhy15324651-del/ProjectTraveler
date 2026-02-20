package org.zerock.projecttraveler.service.reviews;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public Long create(ReviewPostCreateRequest request) {

        require(request.getTravelType() != null && !request.getTravelType().isBlank(), "여행 유형을 선택해주세요.");
        require(request.getThemes() != null && !request.getThemes().isEmpty(), "테마를 1개 이상 선택해주세요.");
        require(request.getPeriod() != null && !request.getPeriod().isBlank(), "기간을 선택해주세요.");
        require(request.getLevel() != null && !request.getLevel().isBlank(), "난이도를 선택해주세요.");
        require(request.getRegionTags() != null && !request.getRegionTags().isEmpty(), "지역을 1개 이상 선택해주세요.");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User writer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자 정보를 찾을 수 없습니다: " + username));

        // ✅ id가 있으면 수정
        if (request.getId() != null) {
            ReviewPost post = reviewPostRepository.findById(request.getId())
                    .orElseThrow(() -> new NoSuchElementException("ReviewPost not found: " + request.getId()));

            // ✅ 작성자만 수정 가능
            if (post.getWriter() == null || !post.getWriter().getId().equals(writer.getId())) {
                throw new IllegalArgumentException("수정 권한이 없습니다.");
            }

            // ✅ 값 반영
            post.setTitle(request.getTitle());
            post.setContent(request.getContent());
            post.setTravelType(request.getTravelType());
            post.setThemes(request.getThemes());      // ✅ 변경
            post.setPeriod(request.getPeriod());
            post.setLevel(request.getLevel());
            post.setRegionTags(request.getRegionTags());
            post.setBudgetFlight(request.getBudgetFlight());
            post.setBudgetLodging(request.getBudgetLodging());
            post.setBudgetFood(request.getBudgetFood());
            post.setBudgetExtra(request.getBudgetExtra());

            return post.getId();
        }

        // ✅ id 없으면 신규 생성
        ReviewPost post = ReviewPost.builder()
                .writer(writer)
                .title(request.getTitle())
                .content(request.getContent())
                .travelType(request.getTravelType())
                .themes(request.getThemes())           // ✅ 변경
                .period(request.getPeriod())
                .level(request.getLevel())
                .regionTags(request.getRegionTags())
                .budgetFlight(request.getBudgetFlight())
                .budgetLodging(request.getBudgetLodging())
                .budgetFood(request.getBudgetFood())
                .budgetExtra(request.getBudgetExtra())
                .build();

        return reviewPostRepository.save(post).getId();
    }



    /** * 최신순 목록 조회 (+ 썸네일/요약 추출) */
    public List<ReviewPost> listLatest() {
        List<ReviewPost> posts = reviewPostRepository.findAllByOrderByCreatedAtDesc();
        posts.forEach(this::fillThumbAndSummary);
        return posts;
    }

    /**
     * ✅ 검색 + 페이징 (+ 썸네일/요약)
     * - pageable은 컨트롤러에서 만들어서 전달
     */
    private Specification<ReviewPost> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public Page<ReviewPost> search(ReviewPostSearchRequest req, Pageable pageable) {

        boolean hasTags = req.getTags() != null && !req.getTags().isEmpty();

        Page<ReviewPost> result;

        if (hasTags) {
            result = reviewPostRepository.searchWithRegionPriority(req, pageable);
        } else {
            Specification<ReviewPost> spec = alwaysTrue()
                    .and(ReviewPostSpecs.notDeleted())
                    .and(ReviewPostSpecs.keyword(req.getQ()))
                    .and(ReviewPostSpecs.travelTypeEq(req.getTravelType()))
                    .and(ReviewPostSpecs.themeIn(req.getThemes()))
                    .and(ReviewPostSpecs.periodIn(req.getPeriods()))
                    .and(ReviewPostSpecs.levelIn(req.getLevels()))
                    .and(ReviewPostSpecs.budgetTotalBetween(req.getMinBudget(), req.getMaxBudget()));

            result = reviewPostRepository.findAll(spec, pageable);
        }

        // ✅ 공통 가공
        result.getContent().forEach(p -> {
            fillThumbAndSummary(p);

            // ✅ 지역 선택이 있을 때만 "일치 개수" 계산
            if (hasTags) {
                int cnt = 0;
                List<String> postTags = p.getRegionTags();
                for (String selected : req.getTags()) {
                    if (postTags != null && postTags.contains(selected)) cnt++;
                }
                p.setRegionMatchCount(cnt);
            } else {
                p.setRegionMatchCount(null);
            }
        });

        return result;
    }


    /** * 단건 조회 (+ 썸네일/요약 추출) */
    public ReviewPost findById(Long id) {
        ReviewPost post = reviewPostRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ReviewPost not found: " + id));

        if (Boolean.TRUE.equals(post.isDeleted())) {
            throw new NoSuchElementException("삭제된 게시글입니다: " + id);
        }

        fillThumbAndSummary(post);
        return post;
    }


    /** * 공통: 썸네일/요약 채우기 */
    private void fillThumbAndSummary(ReviewPost p) {
        p.setThumbnailUrl(extractFirstImageUrl(p.getContent()));
        p.setSummary(extractTextOnly(p.getContent()));
    }


    /** * HTML에서 텍스트만 추출 후 120자 요약 */
    private String extractTextOnly(String html) {
        if (html == null || html.isBlank()) return "";
        String text = Jsoup.parse(html).text();
        text = text.replace("\u00A0", " ").trim();
        return text.length() > 120 ? text.substring(0, 120) + "..." : text;
    }


    /** * 본문(HTML)에서 첫 번째 이미지 src 추출 */
    private String extractFirstImageUrl(String html) {
        if (html == null || html.isBlank()) return null;

        Document doc = Jsoup.parse(html);
        Element img = doc.selectFirst("img");
        if (img == null) return null;

        String src = img.attr("src");
        return (src == null || src.isBlank()) ? null : src;
    }

    @Transactional
    public void softDelete(Long id) {
        ReviewPost post = reviewPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음: " + id));

        post.setDeleted(true); // 또는 setIsDeleted(true) - 네 필드명에 맞춰서
    }

}
