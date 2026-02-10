package org.zerock.projecttraveler.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.entity.*;
import org.zerock.projecttraveler.repository.*;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.CourseService;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/quiz")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminQuizController {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizOptionRepository optionRepository;
    private final CourseService courseService;
    private final LessonRepository lessonRepository;

    /**
     * 강좌의 퀴즈 관리 페이지
     */
    @GetMapping("/course/{courseId}")
    public String quizManagementPage(@PathVariable Long courseId, Model model) {
        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        List<Quiz> quizzes = quizRepository.findAllByCourseIdWithLesson(courseId);
        List<Lesson> lessons = courseService.findLessonsByCourseId(courseId);

        model.addAttribute("course", course);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("lessons", lessons);
        model.addAttribute("activePage", "admin-courses");
        model.addAttribute("username", SecurityUtils.getCurrentUserDetails()
                .map(u -> u.getFullName()).orElse("관리자"));
        model.addAttribute("isAdmin", true);

        return "admin/quiz-list";
    }

    /**
     * 퀴즈 상세 (문제 관리)
     */
    @GetMapping("/{quizId}")
    public String quizDetailPage(@PathVariable Long quizId, Model model) {
        Quiz quiz = quizRepository.findByIdWithQuestionsAndOptions(quizId)
                .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다."));

        model.addAttribute("quiz", quiz);
        model.addAttribute("course", quiz.getCourse());
        model.addAttribute("activePage", "admin-courses");
        model.addAttribute("username", SecurityUtils.getCurrentUserDetails()
                .map(u -> u.getFullName()).orElse("관리자"));
        model.addAttribute("isAdmin", true);

        return "admin/quiz-detail";
    }

    // ===== API Endpoints =====

    /**
     * 퀴즈 생성
     */
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<ApiResponse<Quiz>> createQuiz(@Valid @RequestBody QuizRequest request) {
        try {
            Course course = courseService.findById(request.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

            Lesson lesson = null;
            if (request.getLessonId() != null) {
                lesson = lessonRepository.findById(request.getLessonId())
                        .orElseThrow(() -> new IllegalArgumentException("레슨을 찾을 수 없습니다."));
            }

            Quiz quiz = Quiz.builder()
                    .course(course)
                    .lesson(lesson)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .passingScore(request.getPassingScore() != null ? request.getPassingScore() : 80)
                    .timeLimitSec(request.getTimeLimitSec())
                    .active(true)
                    .build();

            Quiz saved = quizRepository.save(quiz);
            log.info("Quiz created: id={}, title={}, courseId={}, lessonId={}",
                    saved.getId(), saved.getTitle(), course.getId(), request.getLessonId());

            return ResponseEntity.ok(ApiResponse.success("퀴즈가 생성되었습니다.", saved));
        } catch (Exception e) {
            log.error("Failed to create quiz", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 퀴즈 수정
     */
    @PutMapping("/api/{quizId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Quiz>> updateQuiz(
            @PathVariable Long quizId,
            @Valid @RequestBody QuizRequest request) {
        try {
            Quiz quiz = quizRepository.findById(quizId)
                    .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다."));

            Lesson lesson = null;
            if (request.getLessonId() != null) {
                lesson = lessonRepository.findById(request.getLessonId())
                        .orElseThrow(() -> new IllegalArgumentException("레슨을 찾을 수 없습니다."));
            }

            quiz.setLesson(lesson);
            quiz.setTitle(request.getTitle());
            quiz.setDescription(request.getDescription());
            if (request.getPassingScore() != null) {
                quiz.setPassingScore(request.getPassingScore());
            }
            quiz.setTimeLimitSec(request.getTimeLimitSec());

            Quiz saved = quizRepository.save(quiz);
            return ResponseEntity.ok(ApiResponse.success("퀴즈가 수정되었습니다.", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 퀴즈 삭제 (비활성화)
     */
    @DeleteMapping("/api/{quizId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(@PathVariable Long quizId) {
        try {
            Quiz quiz = quizRepository.findById(quizId)
                    .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다."));

            quiz.setActive(false);
            quizRepository.save(quiz);

            log.info("Quiz deactivated: id={}", quizId);
            return ResponseEntity.ok(ApiResponse.success("퀴즈가 삭제되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 문제 추가
     */
    @PostMapping("/api/{quizId}/questions")
    @ResponseBody
    public ResponseEntity<ApiResponse<QuizQuestion>> addQuestion(
            @PathVariable Long quizId,
            @Valid @RequestBody QuestionRequest request) {
        try {
            Quiz quiz = quizRepository.findById(quizId)
                    .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다."));

            int nextOrder = questionRepository.countByQuizId(quizId) + 1;

            QuizQuestion question = QuizQuestion.builder()
                    .quiz(quiz)
                    .question(request.getQuestion())
                    .questionType(request.getQuestionType() != null ?
                            request.getQuestionType() : QuizQuestion.QuestionType.MULTIPLE_CHOICE)
                    .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : nextOrder)
                    .points(request.getPoints() != null ? request.getPoints() : 1)
                    .build();

            QuizQuestion saved = questionRepository.save(question);

            // 선택지 추가
            if (request.getOptions() != null && !request.getOptions().isEmpty()) {
                int optionOrder = 1;
                for (OptionRequest optReq : request.getOptions()) {
                    QuizOption option = QuizOption.builder()
                            .question(saved)
                            .content(optReq.getContent())
                            .isCorrect(optReq.getIsCorrect() != null ? optReq.getIsCorrect() : false)
                            .sortOrder(optReq.getSortOrder() != null ? optReq.getSortOrder() : optionOrder++)
                            .build();
                    optionRepository.save(option);
                }
            }

            log.info("Question added: quizId={}, questionId={}", quizId, saved.getId());
            return ResponseEntity.ok(ApiResponse.success("문제가 추가되었습니다.", saved));
        } catch (Exception e) {
            log.error("Failed to add question", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 문제 수정
     */
    @PutMapping("/api/questions/{questionId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<QuizQuestion>> updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionRequest request) {
        try {
            QuizQuestion question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다."));

            question.setQuestion(request.getQuestion());
            if (request.getQuestionType() != null) {
                question.setQuestionType(request.getQuestionType());
            }
            if (request.getSortOrder() != null) {
                question.setSortOrder(request.getSortOrder());
            }
            if (request.getPoints() != null) {
                question.setPoints(request.getPoints());
            }

            QuizQuestion saved = questionRepository.save(question);

            // 선택지 업데이트 (기존 삭제 후 새로 추가)
            if (request.getOptions() != null) {
                // 기존 선택지 삭제
                List<QuizOption> existingOptions = optionRepository.findByQuestionIdOrderBySortOrderAsc(questionId);
                optionRepository.deleteAll(existingOptions);

                // 새 선택지 추가
                int optionOrder = 1;
                for (OptionRequest optReq : request.getOptions()) {
                    QuizOption option = QuizOption.builder()
                            .question(saved)
                            .content(optReq.getContent())
                            .isCorrect(optReq.getIsCorrect() != null ? optReq.getIsCorrect() : false)
                            .sortOrder(optReq.getSortOrder() != null ? optReq.getSortOrder() : optionOrder++)
                            .build();
                    optionRepository.save(option);
                }
            }

            return ResponseEntity.ok(ApiResponse.success("문제가 수정되었습니다.", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 문제 삭제
     */
    @DeleteMapping("/api/questions/{questionId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Long questionId) {
        try {
            QuizQuestion question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다."));

            // 선택지 먼저 삭제
            List<QuizOption> options = optionRepository.findByQuestionIdOrderBySortOrderAsc(questionId);
            optionRepository.deleteAll(options);

            // 문제 삭제
            questionRepository.delete(question);

            log.info("Question deleted: id={}", questionId);
            return ResponseEntity.ok(ApiResponse.success("문제가 삭제되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 선택지 추가
     */
    @PostMapping("/api/questions/{questionId}/options")
    @ResponseBody
    public ResponseEntity<ApiResponse<QuizOption>> addOption(
            @PathVariable Long questionId,
            @Valid @RequestBody OptionRequest request) {
        try {
            QuizQuestion question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다."));

            List<QuizOption> existingOptions = optionRepository.findByQuestionIdOrderBySortOrderAsc(questionId);
            int nextOrder = existingOptions.size() + 1;

            QuizOption option = QuizOption.builder()
                    .question(question)
                    .content(request.getContent())
                    .isCorrect(request.getIsCorrect() != null ? request.getIsCorrect() : false)
                    .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : nextOrder)
                    .build();

            QuizOption saved = optionRepository.save(option);
            return ResponseEntity.ok(ApiResponse.success("선택지가 추가되었습니다.", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 선택지 수정
     */
    @PutMapping("/api/options/{optionId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<QuizOption>> updateOption(
            @PathVariable Long optionId,
            @Valid @RequestBody OptionRequest request) {
        try {
            QuizOption option = optionRepository.findById(optionId)
                    .orElseThrow(() -> new IllegalArgumentException("선택지를 찾을 수 없습니다."));

            option.setContent(request.getContent());
            if (request.getIsCorrect() != null) {
                option.setIsCorrect(request.getIsCorrect());
            }
            if (request.getSortOrder() != null) {
                option.setSortOrder(request.getSortOrder());
            }

            QuizOption saved = optionRepository.save(option);
            return ResponseEntity.ok(ApiResponse.success("선택지가 수정되었습니다.", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 선택지 삭제
     */
    @DeleteMapping("/api/options/{optionId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteOption(@PathVariable Long optionId) {
        try {
            QuizOption option = optionRepository.findById(optionId)
                    .orElseThrow(() -> new IllegalArgumentException("선택지를 찾을 수 없습니다."));

            optionRepository.delete(option);
            return ResponseEntity.ok(ApiResponse.success("선택지가 삭제되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ===== Request DTOs =====

    @Data
    public static class QuizRequest {
        @NotNull(message = "강좌 ID는 필수입니다.")
        private Long courseId;
        private Long lessonId;  // null이면 강좌 전체 퀴즈
        @NotBlank(message = "퀴즈 제목은 필수입니다.")
        private String title;
        private String description;
        private Integer passingScore;  // 기본값 80
        private Integer timeLimitSec;
    }

    @Data
    public static class QuestionRequest {
        @NotBlank(message = "문제 내용은 필수입니다.")
        private String question;
        private QuizQuestion.QuestionType questionType;
        private Integer sortOrder;
        private Integer points;
        private List<OptionRequest> options;
    }

    @Data
    public static class OptionRequest {
        @NotBlank(message = "선택지 내용은 필수입니다.")
        private String content;
        private Boolean isCorrect;
        private Integer sortOrder;
    }
}
