package org.zerock.projecttraveler.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.dto.MyLearningSummaryDto;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardService dashboardService;

    /**
     * 나의 학습 요약 정보 조회
     * 메인 화면 "나의 수강현황" 및 마이페이지에서 사용
     */
    @GetMapping("/my-learning")
    public ResponseEntity<ApiResponse<MyLearningSummaryDto>> getMyLearning() {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        MyLearningSummaryDto summary = dashboardService.getMyLearningSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
