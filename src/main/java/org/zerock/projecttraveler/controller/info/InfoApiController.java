package org.zerock.projecttraveler.controller.info;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.info.InfoPostCardDto;
import org.zerock.projecttraveler.dto.info.InfoPostDto;
import org.zerock.projecttraveler.service.info.InfoPostService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/info")
public class InfoApiController {

    private final InfoPostService infoPostService;

    // ✅ 카드 목록
    // 예) /api/info/cards?regionKey=asahikawa&tabType=FOOD
    @GetMapping("/cards")
    public List<InfoPostCardDto> listCards(
            @RequestParam String regionKey,
            @RequestParam String tabType
    ) {
        return infoPostService.listCards(regionKey, tabType);
    }


    // ✅ 카드 클릭 시 상세 조회
    @GetMapping("/posts/{postKey}")
    public InfoPostDto getPost(@PathVariable String postKey) {
        return infoPostService.getPostDto(postKey);
    }
}
