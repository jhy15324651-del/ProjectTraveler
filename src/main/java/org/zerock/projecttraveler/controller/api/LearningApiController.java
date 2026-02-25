package org.zerock.projecttraveler.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.dto.LearningCompleteRequest;
import org.zerock.projecttraveler.dto.LearningHeartbeatRequest;
import org.zerock.projecttraveler.entity.LessonProgress;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.CertificateService;
import org.zerock.projecttraveler.service.LearningService;
import org.zerock.projecttraveler.service.QuizService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
@Slf4j
public class LearningApiController {

    private final LearningService learningService;
    private final CertificateService certificateService;
    private final QuizService quizService;

    /**
     * Heartbeat - 영상 학습 추적 (10~15초마다 호출)
     * 중요: userId는 클라이언트가 아닌 서버에서 SecurityContext로 강제
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(@Valid @RequestBody LearningHeartbeatRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        try {
            learningService.heartbeat(
                    userId,
                    request.getCourseId(),
                    request.getLessonId(),
                    request.getPositionSec(),
                    request.getDeltaWatchedSec()
            );
            return ResponseEntity.ok(ApiResponse.success("학습 기록이 저장되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 레슨 완료 처리
     */
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> complete(@Valid @RequestBody LearningCompleteRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        try {
            boolean completed = learningService.complete(
                    userId,
                    request.getCourseId(),
                    request.getLessonId()
            );

            Map<String, Object> data = new HashMap<>();
            data.put("completed", completed);

            if (completed) {
                quizService.tryCompleteRetake(userId, request.getCourseId());
                certificateService.tryIssueCertificate(userId, request.getCourseId());
                return ResponseEntity.ok(ApiResponse.success("레슨을 완료했습니다.", data));
            } else {
                return ResponseEntity.ok(ApiResponse.success("아직 완료 조건을 충족하지 못했습니다.", data));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 레슨 진도 조회
     */
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProgress(
            @RequestParam Long courseId,
            @RequestParam Long lessonId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        LessonProgress progress = learningService.getLessonProgress(userId, lessonId);

        Map<String, Object> data = new HashMap<>();
        if (progress != null) {
            data.put("lastPositionSec", progress.getLastPositionSec());
            data.put("watchedSec", progress.getWatchedSec());
            data.put("completed", progress.getCompleted());
            data.put("progressPercent", progress.getProgressPercent());
        } else {
            data.put("lastPositionSec", 0);
            data.put("watchedSec", 0);
            data.put("completed", false);
            data.put("progressPercent", 0);
        }

        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
