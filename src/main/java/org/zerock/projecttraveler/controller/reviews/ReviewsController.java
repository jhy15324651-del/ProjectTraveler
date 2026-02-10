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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        int size = 5;
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // =========================
    // LIST
    // =========================
    @GetMapping("/reviews")
    public String reviewsWeb(@ModelAttribute("search") ReviewPostSearchRequest req, Model model) {
        applyCommonModel(model);
        applyFilterOptions(model);
        model.addAttribute("isUnity", false);

        Page<ReviewPost> pageResult = reviewPostService.search(req, buildPageableFixed(req));
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("posts", pageResult.getContent());

        return "reviews/reviews-web";
    }

    @GetMapping("/reviews-unity")
    public String reviewsUnity(@ModelAttribute("search") ReviewPostSearchRequest req, Model model) {
        applyCommonModel(model);
        applyFilterOptions(model);
        model.addAttribute("isUnity", true);

        Page<ReviewPost> pageResult = reviewPostService.search(req, buildPageableFixed(req));
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("posts", pageResult.getContent());

        return "reviews/reviews-unity";
    }

    // =========================
    // CREATE (POST 화면) - ✅ edit 코드 제거
    // =========================
    @GetMapping("/reviews-post")
    public String reviewsPostWeb(Model model,
                                 @ModelAttribute("request") ReviewPostCreateRequest request) {
        applyCommonModel(model);
        applyFilterOptions(model);
        model.addAttribute("isUnity", false);

        return "reviews/reviews-post-web";
    }

    @GetMapping("/reviews-unity-post")
    public String reviewsPostUnity(Model model,
                                   @ModelAttribute("request") ReviewPostCreateRequest request) {
        applyCommonModel(model);
        applyFilterOptions(model);
        model.addAttribute("isUnity", true);

        return "reviews/reviews-post-unity";
    }

    // =========================
    // EDIT (수정 화면) - ✅ 새로 분리
    // - wrapper: reviews-edit-web / reviews-edit-unity
    // - fragment: reviews-edit.html (조립식)
    // =========================
    @GetMapping("/reviews/{id}/edit")
    public String editWeb(@PathVariable Long id, Model model,
                          @ModelAttribute("request") ReviewPostCreateRequest request) {
        applyCommonModel(model);
        applyFilterOptions(model);
        model.addAttribute("isUnity", false);

        ReviewPost post = reviewPostService.findById(id);

        // ✅ 작성자만 접근
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);
        boolean isAuthor = (user != null
                && post.getWriter() != null
                && post.getWriter().getId() != null
                && post.getWriter().getId().equals(user.getId()));

        if (!isAuthor) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "수정 권한이 없습니다."
            );
        }

        // ✅ request 프리필
        request.setId(post.getId());
        request.setTitle(post.getTitle());
        request.setContent(post.getContent());
        request.setTravelType(post.getTravelType());
        request.setTheme(post.getTheme());
        request.setPeriod(post.getPeriod());
        request.setLevel(post.getLevel());
        request.setRegionTags(post.getRegionTags());
        request.setBudgetFlight(post.getBudgetFlight());
        request.setBudgetLodging(post.getBudgetLodging());
        request.setBudgetFood(post.getBudgetFood());
        request.setBudgetExtra(post.getBudgetExtra());

        return "reviews/reviews-edit-web";
    }

    @GetMapping("/reviews-unity/{id}/edit")
    public String editUnity(@PathVariable Long id, Model model,
                            @ModelAttribute("request") ReviewPostCreateRequest request) {
        applyCommonModel(model);
        applyFilterOptions(model);
        model.addAttribute("isUnity", true);

        ReviewPost post = reviewPostService.findById(id);

        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);
        boolean isAuthor = (user != null
                && post.getWriter() != null
                && post.getWriter().getId() != null
                && post.getWriter().getId().equals(user.getId()));

        if (!isAuthor) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "수정 권한이 없습니다."
            );
        }

        request.setId(post.getId());
        request.setTitle(post.getTitle());
        request.setContent(post.getContent());
        request.setTravelType(post.getTravelType());
        request.setTheme(post.getTheme());
        request.setPeriod(post.getPeriod());
        request.setLevel(post.getLevel());
        request.setRegionTags(post.getRegionTags());
        request.setBudgetFlight(post.getBudgetFlight());
        request.setBudgetLodging(post.getBudgetLodging());
        request.setBudgetFood(post.getBudgetFood());
        request.setBudgetExtra(post.getBudgetExtra());

        return "reviews/reviews-edit-unity";
    }

    // =========================
    // SAVE (create/update 공용) - 기존 유지
    // =========================
    @PostMapping("/reviews")
    public String create(@Valid @ModelAttribute("request") ReviewPostCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         @RequestParam(name = "fromUnity", defaultValue = "0") String fromUnity) {

        boolean isUnity = "1".equals(fromUnity) || "true".equalsIgnoreCase(fromUnity);

        if (bindingResult.hasErrors()) {
            applyCommonModel(model);
            applyFilterOptions(model);
            model.addAttribute("isUnity", isUnity);

            if (request.getId() != null) {
                return isUnity ? "reviews/reviews-edit-unity" : "reviews/reviews-edit-web";
            }
            return isUnity ? "reviews/reviews-post-unity" : "reviews/reviews-post-web";
        }

        Long savedId = reviewPostService.create(request);

        // ✅ 수정이면 상세로
        if (request.getId() != null) {
            return isUnity ? "redirect:/reviews-unity/" + savedId
                    : "redirect:/reviews/" + savedId;
        }

        // ✅ 작성이면 목록으로
        return isUnity ? "redirect:/reviews-unity" : "redirect:/reviews";
    }


    // =========================
    // READ
    // =========================
    @GetMapping("/reviews/{id}")
    public String readWeb(@PathVariable Long id, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", false);

        ReviewPost post = reviewPostService.findById(id);
        model.addAttribute("post", post);

        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);
        boolean isAuthor = (user != null
                && post.getWriter() != null
                && post.getWriter().getId() != null
                && post.getWriter().getId().equals(user.getId()));
        boolean isAdmin = SecurityUtils.isAdmin();

        model.addAttribute("canEdit", isAuthor);
        model.addAttribute("canDelete", isAuthor || isAdmin);

        return "reviews/reviews-read-web";
    }

    @GetMapping("/reviews-unity/{id}")
    public String readUnity(@PathVariable Long id, Model model) {
        applyCommonModel(model);
        model.addAttribute("isUnity", true);

        ReviewPost post = reviewPostService.findById(id);
        model.addAttribute("post", post);

        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);
        boolean isAuthor = (user != null
                && post.getWriter() != null
                && post.getWriter().getId() != null
                && post.getWriter().getId().equals(user.getId()));
        boolean isAdmin = SecurityUtils.isAdmin();

        model.addAttribute("canEdit", isAuthor);
        model.addAttribute("canDelete", isAuthor || isAdmin);

        return "reviews/reviews-read-unity";
    }

    // =========================
    // DELETE (소프트) - 기존 유지
    // =========================
    @PostMapping("/reviews/{id}/delete")
    public String deleteWeb(@PathVariable Long id) {
        ReviewPost post = reviewPostService.findById(id);

        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);
        boolean isAuthor = (user != null
                && post.getWriter() != null
                && post.getWriter().getId() != null
                && post.getWriter().getId().equals(user.getId()));
        boolean isAdmin = SecurityUtils.isAdmin();

        if (!(isAuthor || isAdmin)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "삭제 권한이 없습니다."
            );
        }

        reviewPostService.softDelete(id);
        return "redirect:/reviews";
    }

    @PostMapping("/reviews-unity/{id}/delete")
    public String deleteUnity(@PathVariable Long id) {
        ReviewPost post = reviewPostService.findById(id);

        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);
        boolean isAuthor = (user != null
                && post.getWriter() != null
                && post.getWriter().getId() != null
                && post.getWriter().getId().equals(user.getId()));
        boolean isAdmin = SecurityUtils.isAdmin();

        if (!(isAuthor || isAdmin)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "삭제 권한이 없습니다."
            );
        }

        reviewPostService.softDelete(id);
        return "redirect:/reviews-unity";
    }

    // =========================
    // FILTER OPTIONS
    // =========================
    private void applyFilterOptions(Model model) {
        Map<String, List<String>> regionGroups = new LinkedHashMap<>();
        regionGroups.put("홋카이도", List.of("아사히카와", "삿포로", "하코다테"));
        regionGroups.put("혼슈", List.of("도쿄", "오사카", "나고야", "히로시마", "교토"));
        regionGroups.put("시코쿠", List.of("고치", "마쓰야마", "다카마쓰"));
        regionGroups.put("큐슈", List.of("기타큐슈", "나가사키", "구마모토", "후쿠오카", "오키나와"));
        model.addAttribute("regionGroups", regionGroups);
    }
}
