package org.zerock.projecttraveler.controller.info;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.info.InfoPostReorderRequest;
import org.zerock.projecttraveler.service.info.InfoPostService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/info")
public class AdminInfoOrderApiController {

    private final InfoPostService infoPostService;

    // ✅ 순서 변경 저장
    // PUT /api/admin/info/reorder?regionKey=asahikawa&tabType=FOOD
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reorder")
    public void reorder(
            @RequestParam String regionKey,
            @RequestParam String tabType,
            @RequestBody InfoPostReorderRequest request
    ) {
        infoPostService.reorder(regionKey, tabType, request);
    }
}
