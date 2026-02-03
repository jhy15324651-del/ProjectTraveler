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

    private void applyCommonModel(Model model) {
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);
        model.addAttribute("activePage", "reviews");
        model.addAttribute("username", user != null ? user.getFullName() : "사용자");
        model.addAttribute("isAdmin", SecurityUtils.isAdmin());
    }

    private Pageable buildPageableFixed(ReviewPostSearchRequest req) {
        int page = (req.getPage() == null || req.getPage() < 0) ? 0 : req.getPage();
        int size = 5; // ✅ 고정
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @GetMapping("/reviews")
    public String reviewsWeb(@ModelAttribute("search") ReviewPostSearchRequest req, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", false);

        Page<ReviewPost> pageResult = reviewPostService.search(req, buildPageableFixed(req));
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("posts", pageResult.getContent());

        return "reviews-web";
    }

    @GetMapping("/reviews-unity")
    public String reviewsUnity(@ModelAttribute("search") ReviewPostSearchRequest req, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", true);

        Page<ReviewPost> pageResult = reviewPostService.search(req, buildPageableFixed(req));
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("posts", pageResult.getContent());

        return "reviews-unity";
    }

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

    @GetMapping("/reviews/{id}")
    public String readWeb(@PathVariable Long id, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", false);
        model.addAttribute("post", reviewPostService.findById(id));
        return "reviews-read-web";
    }

    @GetMapping("/reviews-unity/{id}")
    public String readUnity(@PathVariable Long id, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", true);
        model.addAttribute("post", reviewPostService.findById(id));
        return "reviews-read-unity";
    }
}

