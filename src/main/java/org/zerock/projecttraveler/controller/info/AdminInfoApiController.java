package org.zerock.projecttraveler.controller.info;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.info.InfoPostCreateRequest;
import org.zerock.projecttraveler.dto.info.InfoPostReorderRequest;
import org.zerock.projecttraveler.dto.info.InfoPostUpsertRequest;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.info.InfoPostService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/info")
@RequiredArgsConstructor
public class AdminInfoApiController {

    private final InfoPostService infoPostService;

    private String adminUsername() {
        return SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new IllegalStateException("로그인이 필요합니다."));
    }

    // ✅ 업서트(본문 포함)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/posts/{postKey}")
    public void upsert(@PathVariable String postKey,
                       @RequestBody InfoPostUpsertRequest request) {
        infoPostService.upsertPost(postKey, request, adminUsername());
    }

    // ✅ 생성
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/posts")
    public Map<String, String> create(@RequestBody InfoPostCreateRequest req) {
        String postKey = infoPostService.createPost(req, adminUsername());
        return Map.of("postKey", postKey);
    }

    // ✅ 삭제
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/posts/{postKey}")
    public void delete(@PathVariable String postKey) {
        infoPostService.deletePost(postKey);
    }

    // ✅ 순서 저장
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/posts/reorder")
    public void reorder(@RequestParam String regionKey,
                        @RequestParam String tabType,
                        @RequestBody InfoPostReorderRequest req) {
        infoPostService.reorder(regionKey, tabType, req);
    }
}
