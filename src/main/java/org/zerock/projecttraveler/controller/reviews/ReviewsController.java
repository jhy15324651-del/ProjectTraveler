package org.zerock.projecttraveler.controller.reviews;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
     * ✅ 대표(웹) 후기 목록
     * GET /reviews
     * - reviews.html은 fragment이므로 문서 템플릿인 reviews-web을 반환
     */
    @GetMapping("/reviews")
    public String reviewsWeb(ReviewPostSearchRequest req, Model model) {
        applyCommonModel(model);

        Page<ReviewPost> pageResult = reviewPostService.search(req);
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("posts", pageResult.getContent());   // ✅ 추가: 템플릿 호환
        model.addAttribute("search", req);

        return "reviews-web";
    }

    /**
     * ✅ 유니티 전용 후기 목록(헤더 없음)
     * GET /reviews-unity
     */
    @GetMapping("/reviews-unity")
    public String reviewsUnity(ReviewPostSearchRequest req, Model model) {
        applyCommonModel(model);

        Page<ReviewPost> pageResult = reviewPostService.search(req);
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("posts", pageResult.getContent());   // ✅ 추가: 템플릿 호환
        model.addAttribute("search", req);

        return "reviews-unity";
    }

    /**
     * ✅ 후기 작성 페이지
     * GET /reviews-post
     */
    @GetMapping("/reviews-post")
    public String reviewsPost(Model model,
                              @ModelAttribute("request") ReviewPostCreateRequest request) {
        applyCommonModel(model);
        // request는 폼 바인딩용(빈 객체) - 이미 @ModelAttribute로 올라감
        return "reviews-post";
    }

    /**
     * ✅ 후기 저장
     * POST /reviews
     */
    @PostMapping("/reviews")
    public String create(@Valid @ModelAttribute("request") ReviewPostCreateRequest request,
                         BindingResult bindingResult,
                         Model model) {

        // 필수값 검증 실패 시 작성 페이지로 복귀
        if (bindingResult.hasErrors()) {
            applyCommonModel(model);
            return "reviews-post";
        }

        reviewPostService.create(request);
        return "redirect:/reviews";
    }

    /**
     * ✅ 후기 상세 페이지
     * GET /reviews/{id}
     */
    @GetMapping("/reviews/{id}")
    public String read(@PathVariable Long id, Model model) {
        applyCommonModel(model);

        model.addAttribute("post", reviewPostService.findById(id));
        return "reviews-read";
    }
}
