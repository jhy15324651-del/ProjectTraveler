package org.zerock.projecttraveler.controller.reviews;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.zerock.projecttraveler.service.reviews.ReviewPostService;

@Controller
@RequiredArgsConstructor
public class ReviewReadController {

    private final ReviewPostService reviewPostService;

    @GetMapping("/reviews/{id}")
    public String read(@PathVariable Long id, Model model) {

        model.addAttribute("post", reviewPostService.findById(id));
        return "reviews-read"; // ✅ 다음 단계에서 templates/reviews-read.html 만들 예정
    }
}
