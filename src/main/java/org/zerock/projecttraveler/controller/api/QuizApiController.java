package org.zerock.projecttraveler.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.dto.QuizDto;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.CertificateService;
import org.zerock.projecttraveler.service.QuizService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizApiController {

    private final QuizService quizService;
    private final CertificateService certificateService;

    /**
     * 강좌의 퀴즈 조회 (문제 포함, 정답 미포함)
     * GET /api/quiz/course/{courseId}
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getQuizForCourse(@PathVariable Long courseId) {
        return quizService.getQuizForCourse(courseId)
                .map(quiz -> ResponseEntity.ok(ApiResponse.success(quiz)))
                .orElse(ResponseEntity.ok(ApiResponse.error("이 강좌에는 퀴즈가 없습니다.")));
    }

    /**
     * 퀴즈 상세 조회 (문제 및 선택지 포함, 정답 미포함)
     * GET /api/quiz/{quizId}
     */
    @GetMapping("/{quizId}")
    public ResponseEntity<?> getQuiz(@PathVariable Long quizId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        // 응시 가능 여부 확인
        if (!quizService.canAttemptQuiz(userId, quizId)) {
            var status = quizService.getQuizStatusByQuizId(userId, quizId);
            if (status.isPresent()) {
                return ResponseEntity.status(403).body(ApiResponse.error(
                        status.get().getMessage(),
                        Map.of("quizStatus", status.get())
                ));
            }
            return ResponseEntity.status(403).body(ApiResponse.error("퀴즈에 응시할 수 없습니다."));
        }

        return quizService.getQuizById(quizId)
                .map(quiz -> ResponseEntity.ok(ApiResponse.success(quiz)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 퀴즈 제출
     * POST /api/quiz/submit
     *
     * 요청 예시:
     * {
     *   "quizId": 1,
     *   "answers": [
     *     {"questionId": 1, "selectedOptionId": 3},
     *     {"questionId": 2, "selectedOptionId": 7},
     *     {"questionId": 3, "selectedOptionId": 10}
     *   ]
     * }
     *
     * 응답 예시 (합격):
     * {
     *   "success": true,
     *   "data": {
     *     "attemptId": 5,
     *     "attemptNo": 1,
     *     "cycle": 1,
     *     "totalQuestions": 10,
     *     "correctCount": 9,
     *     "scorePercent": 90,
     *     "passingScore": 80,
     *     "passed": true,
     *     "status": "PASS",
     *     "showReview": false,
     *     "answerResults": [...]
     *   }
     * }
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody QuizDto.SubmitRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        try {
            QuizDto.SubmitResult result = quizService.submitQuiz(userId, request);
            quizService.getCourseIdByQuizId(request.getQuizId())
                    .ifPresent(courseId -> certificateService.tryIssueCertificate(userId, courseId));
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalStateException e) {
            // 응시 불가 상태 (RETAKE_REQUIRED, 이미 합격, 횟수 초과)
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 정답/해설 조회 (1차 실패 후에만 가능)
     * GET /api/quiz/{quizId}/review?attemptNo=1
     */
    @GetMapping("/{quizId}/review")
    public ResponseEntity<?> getReview(
            @PathVariable Long quizId,
            @RequestParam(defaultValue = "1") Integer attemptNo) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        return quizService.getReview(userId, quizId, attemptNo)
                .map(review -> ResponseEntity.ok(ApiResponse.success(review)))
                .orElse(ResponseEntity.status(403).body(
                        ApiResponse.error("정답/해설을 조회할 수 없습니다. 불합격한 시험에서만 조회 가능합니다.")
                ));
    }

    /**
     * 강좌 퀴즈 상태 조회
     * GET /api/quiz/status/{courseId}
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "data": {
     *     "quizId": 1,
     *     "title": "일본어 기초 퀴즈",
     *     "passingScore": 80,
     *     "bestScore": 75,
     *     "hasPassed": false,
     *     "attemptCount": 2,
     *     "currentCycle": 1,
     *     "attemptsInCycle": 2,
     *     "quizStatusCode": "RETAKE_REQUIRED",
     *     "canAttempt": false,
     *     "message": "2차 시험 불합격. 강의를 다시 수강한 후 퀴즈에 응시해주세요."
     *   }
     * }
     */
    @GetMapping("/status/{courseId}")
    public ResponseEntity<?> getQuizStatus(@PathVariable Long courseId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        return quizService.getQuizStatus(userId, courseId)
                .map(status -> ResponseEntity.ok(ApiResponse.success(status)))
                .orElse(ResponseEntity.ok(ApiResponse.error("이 강좌에는 퀴즈가 없습니다.")));
    }

    /**
     * 퀴즈 시도 기록 조회
     * GET /api/quiz/{quizId}/history
     */
    @GetMapping("/{quizId}/history")
    public ResponseEntity<?> getAttemptHistory(@PathVariable Long quizId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        var history = quizService.getAttemptHistory(userId, quizId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * 재수강 시작
     * POST /api/quiz/retake/{courseId}/start
     */
    @PostMapping("/retake/{courseId}/start")
    public ResponseEntity<?> startRetake(@PathVariable Long courseId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        try {
            boolean result = quizService.startRetake(userId, courseId);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "started", result,
                    "message", "재수강을 시작했습니다. 강의를 다시 수강해주세요."
            )));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 재수강 완료 처리 (진도 조건 충족 후 호출)
     * POST /api/quiz/retake/{courseId}/complete
     */
    @PostMapping("/retake/{courseId}/complete")
    public ResponseEntity<?> completeRetake(@PathVariable Long courseId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        try {
            boolean result = quizService.completeRetake(userId, courseId);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "completed", result,
                    "message", "재수강이 완료되었습니다. 퀴즈에 다시 응시할 수 있습니다."
            )));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 퀴즈별 개별 상태 조회 (사이클 인식)
     * GET /api/quiz/{quizId}/status
     */
    @GetMapping("/{quizId}/status")
    public ResponseEntity<?> getQuizStatusByQuizId(@PathVariable Long quizId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        return quizService.getQuizStatusByQuizId(userId, quizId)
                .map(status -> ResponseEntity.ok(ApiResponse.success(status)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 응시 가능 여부 확인
     * GET /api/quiz/{quizId}/can-attempt
     */
    @GetMapping("/{quizId}/can-attempt")
    public ResponseEntity<?> canAttempt(@PathVariable Long quizId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        boolean canAttempt = quizService.canAttemptQuiz(userId, quizId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("canAttempt", canAttempt)));
    }

    /**
     * 코스의 전체 활성 퀴즈 목록 조회
     * GET /api/quiz/course/{courseId}/all
     */
    @GetMapping("/course/{courseId}/all")
    public ResponseEntity<?> getQuizzesForCourse(@PathVariable Long courseId) {
        List<QuizDto.QuizInfo> quizzes = quizService.getQuizzesForCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success(quizzes));
    }

    /**
     * 레슨의 전체 활성 퀴즈 목록 조회
     * GET /api/quiz/lesson/{lessonId}/all
     */
    @GetMapping("/lesson/{lessonId}/all")
    public ResponseEntity<?> getQuizzesForLesson(@PathVariable Long lessonId) {
        List<QuizDto.QuizInfo> quizzes = quizService.getQuizzesForLesson(lessonId);
        return ResponseEntity.ok(ApiResponse.success(quizzes));
    }
}
