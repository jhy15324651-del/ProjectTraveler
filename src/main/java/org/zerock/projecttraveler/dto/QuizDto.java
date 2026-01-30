package org.zerock.projecttraveler.dto;

import lombok.*;
import org.zerock.projecttraveler.entity.Quiz;
import org.zerock.projecttraveler.entity.QuizQuestion;
import org.zerock.projecttraveler.entity.QuizOption;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class QuizDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizInfo {
        private Long id;
        private Long courseId;
        private String title;
        private String description;
        private Integer passingScore;
        private Integer timeLimitSec;
        private Integer totalQuestions;
        private List<QuestionInfo> questions;

        public static QuizInfo from(Quiz quiz, boolean includeAnswers) {
            return QuizInfo.builder()
                    .id(quiz.getId())
                    .courseId(quiz.getCourse().getId())
                    .title(quiz.getTitle())
                    .description(quiz.getDescription())
                    .passingScore(quiz.getPassingScore())
                    .timeLimitSec(quiz.getTimeLimitSec())
                    .totalQuestions(quiz.getTotalQuestions())
                    .questions(quiz.getQuestions().stream()
                            .map(q -> QuestionInfo.from(q, includeAnswers))
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionInfo {
        private Long id;
        private String question;
        private String questionType;
        private Integer sortOrder;
        private Integer points;
        private List<OptionInfo> options;

        public static QuestionInfo from(QuizQuestion question, boolean includeCorrect) {
            return QuestionInfo.builder()
                    .id(question.getId())
                    .question(question.getQuestion())
                    .questionType(question.getQuestionType().name())
                    .sortOrder(question.getSortOrder())
                    .points(question.getPoints())
                    .options(question.getOptions().stream()
                            .map(o -> OptionInfo.from(o, includeCorrect))
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionInfo {
        private Long id;
        private String content;
        private Integer sortOrder;
        private Boolean isCorrect;

        public static OptionInfo from(QuizOption option, boolean includeCorrect) {
            return OptionInfo.builder()
                    .id(option.getId())
                    .content(option.getContent())
                    .sortOrder(option.getSortOrder())
                    .isCorrect(includeCorrect ? option.getIsCorrect() : null)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitRequest {
        private Long quizId;
        private List<AnswerSubmit> answers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerSubmit {
        private Long questionId;
        private Long selectedOptionId;
        private String textAnswer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitResult {
        private Long attemptId;
        private Integer totalQuestions;
        private Integer correctCount;
        private Integer scorePercent;
        private Integer passingScore;
        private Boolean passed;
        private List<AnswerResult> answerResults;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerResult {
        private Long questionId;
        private Boolean isCorrect;
        private Long correctOptionId;
        private Long selectedOptionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttemptHistory {
        private Long attemptId;
        private Integer scorePercent;
        private Boolean passed;
        private LocalDateTime completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizStatus {
        private Long quizId;
        private String title;
        private Integer bestScore;
        private Boolean hasPassed;
        private Integer attemptCount;
    }
}
