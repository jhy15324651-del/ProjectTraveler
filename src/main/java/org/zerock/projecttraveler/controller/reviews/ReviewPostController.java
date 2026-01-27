package org.zerock.projecttraveler.controller.reviews;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.zerock.projecttraveler.dto.reviews.ReviewPostCreateRequest;
import org.zerock.projecttraveler.service.reviews.ReviewPostService;

@Controller
@RequiredArgsConstructor
public class ReviewPostController {

    private final ReviewPostService reviewPostService;

    /**
     * 작성 페이지에서 넘어온 여행후기 저장
     * POST /reviews
     */
    @PostMapping("/reviews")
    public String create(@Valid @ModelAttribute("request") ReviewPostCreateRequest request,
                         BindingResult bindingResult) {

        // 필수값(title/content) 비어있으면 다시 작성 페이지로
        if (bindingResult.hasErrors()) {
            return "reviews-post";
        }

        reviewPostService.create(request);
        return "redirect:/reviews";
    }
}
