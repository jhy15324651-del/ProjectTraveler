package org.zerock.projecttraveler.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.dto.QuizDto;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.QuizService;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizApiController {

    private final QuizService quizService;

    /**
     * 강좌의 퀴즈 조회
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getQuizForCourse(@PathVariable Long courseId) {
        return quizService.getQuizForCourse(courseId)
                .map(quiz -> ResponseEntity.ok(ApiResponse.success(quiz)))
                .orElse(ResponseEntity.ok(ApiResponse.error("이 강좌에는 퀴즈가 없습니다.")));
    }

    /**
     * 퀴즈 상세 조회
     */
    @GetMapping("/{quizId}")
    public ResponseEntity<?> getQuiz(@PathVariable Long quizId) {
        return quizService.getQuizById(quizId)
                .map(quiz -> ResponseEntity.ok(ApiResponse.success(quiz)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 퀴즈 제출
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody QuizDto.SubmitRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        try {
            QuizDto.SubmitResult result = quizService.submitQuiz(userId, request);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 강좌 퀴즈 상태 조회
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
     */
    @GetMapping("/{quizId}/history")
    public ResponseEntity<?> getAttemptHistory(@PathVariable Long quizId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        var history = quizService.getAttemptHistory(userId, quizId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
