package org.zerock.projecttraveler.controller.reviews;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.reviews.ReviewPostCreateRequest;
import org.zerock.projecttraveler.dto.reviews.ReviewPostSearchRequest;
import org.zerock.projecttraveler.entity.reviews.ReviewPost;
import org.zerock.projecttraveler.security.CustomUserDetails;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.reviews.ReviewPostService;

@Controller
@RequiredArgsConstructor
public class ReviewsController {

    private final ReviewPostService reviewPostService;

    /**
     * ✅ 공통 모델 세팅 (헤더에서 쓰는 값)
     */
    private void applyCommonModel(Model model) {
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);

        model.addAttribute("activePage", "reviews");
        model.addAttribute("username", user != null ? user.getFullName() : "사용자");
        model.addAttribute("isAdmin", SecurityUtils.isAdmin());
    }

    /**
     * ✅ 페이징 기본값/안전 처리 + 기본 정렬(createdAt desc)
     * - req.page, req.size를 그대로 믿지 말고 안전 보정
     */
    private Pageable buildPageable(ReviewPostSearchRequest req) {
        int page = (req.getPage() == null || req.getPage() < 0) ? 0 : req.getPage();
        int size = (req.getSize() == null || req.getSize() <= 0) ? 12 : req.getSize();

        // 너무 큰 size 방지(선택): 서버 보호용
        if (size > 50) size = 50;

        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * ✅ 대표(웹) 후기 목록
     * GET /reviews
     */
    @GetMapping("/reviews")
    public String reviewsWeb(@ModelAttribute("search") ReviewPostSearchRequest req, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", false);

        Pageable pageable = buildPageable(req);
        Page<ReviewPost> pageResult = reviewPostService.search(req, pageable);

        model.addAttribute("pageResult", pageResult);
        model.addAttribute("posts", pageResult.getContent()); // 템플릿 호환 유지

        return "reviews-web";
    }

    /**
     * ✅ 유니티 전용 후기 목록(헤더 없음)
     * GET /reviews-unity
     */
    @GetMapping("/reviews-unity")
    public String reviewsUnity(@ModelAttribute("search") ReviewPostSearchRequest req, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", true);

        Pageable pageable = buildPageable(req);
        Page<ReviewPost> pageResult = reviewPostService.search(req, pageable);

        model.addAttribute("pageResult", pageResult);
        model.addAttribute("posts", pageResult.getContent()); // 템플릿 호환 유지

        return "reviews-unity";
    }

    /**
     * ✅ 후기 작성 페이지 (웹)
     * GET /reviews-post
     */
    @GetMapping("/reviews-post")
    public String reviewsPostWeb(Model model,
                                 @ModelAttribute("request") ReviewPostCreateRequest request) {
        applyCommonModel(model);
        model.addAttribute("isUnity", false);
        return "reviews-post-web";
    }

    /**
     * ✅ 후기 작성 페이지 (유니티)
     * GET /reviews-unity-post
     */
    @GetMapping("/reviews-unity-post")
    public String reviewsPostUnity(Model model,
                                   @ModelAttribute("request") ReviewPostCreateRequest request) {
        applyCommonModel(model);
        model.addAttribute("isUnity", true);
        return "reviews-post-unity";
    }

    /**
     * ✅ 후기 저장
     * POST /reviews
     */
    @PostMapping("/reviews")
    public String create(@Valid @ModelAttribute("request") ReviewPostCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         @RequestParam(name = "fromUnity", defaultValue = "0") String fromUnity) {

        boolean isUnity = "1".equals(fromUnity) || "true".equalsIgnoreCase(fromUnity);

        if (bindingResult.hasErrors()) {
            applyCommonModel(model);
            model.addAttribute("isUnity", isUnity);
            return isUnity ? "reviews-post-unity" : "reviews-post-web";
        }

        reviewPostService.create(request);
        return isUnity ? "redirect:/reviews-unity" : "redirect:/reviews";
    }

    /**
     * ✅ 후기 상세 (웹 대표)
     * GET /reviews/{id}
     */
    @GetMapping("/reviews/{id}")
    public String readWeb(@PathVariable Long id, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", false);
        model.addAttribute("post", reviewPostService.findById(id));
        return "reviews-read-web";
    }

    /**
     * ✅ 후기 상세 (유니티 전용)
     * GET /reviews-unity/{id}
     */
    @GetMapping("/reviews-unity/{id}")
    public String readUnity(@PathVariable Long id, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", true);
        model.addAttribute("post", reviewPostService.findById(id));
        return "reviews-read-unity";
    }
}
