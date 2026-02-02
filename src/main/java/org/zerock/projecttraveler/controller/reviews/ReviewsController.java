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
     */
    @GetMapping("/reviews")
    public String reviewsWeb(ReviewPostSearchRequest req, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", false); // ✅ 추가

        Page<ReviewPost> pageResult = reviewPostService.search(req);
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("posts", pageResult.getContent());   // 템플릿 호환
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
        model.addAttribute("isUnity", true); // ✅ 추가

        Page<ReviewPost> pageResult = reviewPostService.search(req);
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("posts", pageResult.getContent());   // 템플릿 호환
        model.addAttribute("search", req);

        return "reviews-unity";
    }

    /**
     * ✅ 후기 작성 페이지 (일단 기존 유지)
     * GET /reviews-post
     * - post도 나중에 web/unity 분리할 예정이니, 지금은 그대로 둬도 OK
     */
    @GetMapping("/reviews-post")
    public String reviewsPostWeb(Model model,
                                 @ModelAttribute("request") ReviewPostCreateRequest request) {
        applyCommonModel(model);
        model.addAttribute("isUnity", false);
        return "reviews-post-web";
    }

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

        // 검증 실패 시 작성 페이지로 복귀 (web/unity 분기 유지)
        if (bindingResult.hasErrors()) {
            applyCommonModel(model);
            model.addAttribute("isUnity", isUnity);
            return isUnity ? "reviews-post-unity" : "reviews-post-web";
        }

        reviewPostService.create(request);

        // ✅ 성공 시 redirect 분기
        return isUnity ? "redirect:/reviews-unity" : "redirect:/reviews";
    }

    /**
     * ✅ 후기 상세 (웹 대표)
     * GET /reviews/{id}
     */
    @GetMapping("/reviews/{id}")
    public String readWeb(@PathVariable Long id, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", false); // ✅ 추가
        model.addAttribute("post", reviewPostService.findById(id));

        // ✅ 이제 reviews-read는 fragment이므로 web 껍데기를 반환
        return "reviews-read-web";
    }

    /**
     * ✅ 후기 상세 (유니티 전용)
     * GET /reviews-unity/{id}
     */
    @GetMapping("/reviews-unity/{id}")
    public String readUnity(@PathVariable Long id, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", true); // ✅ 추가
        model.addAttribute("post", reviewPostService.findById(id));

        return "reviews-read-unity";
    }
}
