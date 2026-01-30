package org.zerock.projecttraveler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.projecttraveler.dto.QuizDto;
import org.zerock.projecttraveler.entity.*;
import org.zerock.projecttraveler.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizOptionRepository optionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final AttendanceService attendanceService;

    /**
     * 강좌의 퀴즈 조회 (문제 포함, 정답 미포함)
     */
    public Optional<QuizDto.QuizInfo> getQuizForCourse(Long courseId) {
        return quizRepository.findFirstByCourseIdAndActiveTrue(courseId)
                .map(quiz -> {
                    Quiz fullQuiz = quizRepository.findByIdWithQuestionsAndOptions(quiz.getId())
                            .orElse(quiz);
                    return QuizDto.QuizInfo.from(fullQuiz, false);
                });
    }

    /**
     * 퀴즈 상세 조회 (문제 및 선택지 포함, 정답 미포함)
     */
    public Optional<QuizDto.QuizInfo> getQuizById(Long quizId) {
        return quizRepository.findByIdWithQuestionsAndOptions(quizId)
                .map(quiz -> QuizDto.QuizInfo.from(quiz, false));
    }

    /**
     * 강좌에 퀴즈가 있는지 확인
     */
    public boolean hasQuiz(Long courseId) {
        return quizRepository.existsByCourseIdAndActiveTrue(courseId);
    }

    /**
     * 퀴즈 제출 및 채점
     */
    @Transactional
    public QuizDto.SubmitResult submitQuiz(Long userId, QuizDto.SubmitRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Quiz quiz = quizRepository.findByIdWithQuestionsAndOptions(request.getQuizId())
                .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다."));

        // 정답 맵 생성 (questionId -> correctOptionId)
        Map<Long, Long> correctAnswers = quiz.getQuestions().stream()
                .collect(Collectors.toMap(
                        QuizQuestion::getId,
                        q -> q.getOptions().stream()
                                .filter(QuizOption::getIsCorrect)
                                .findFirst()
                                .map(QuizOption::getId)
                                .orElse(-1L)
                ));

        // Attempt 생성
        QuizAttempt attempt = QuizAttempt.builder()
                .user(user)
                .quiz(quiz)
                .totalQuestions(quiz.getTotalQuestions())
                .startedAt(LocalDateTime.now())
                .build();
        attempt = attemptRepository.save(attempt);

        // 답안 채점
        int correctCount = 0;
        List<QuizDto.AnswerResult> answerResults = new ArrayList<>();

        for (QuizDto.AnswerSubmit answerSubmit : request.getAnswers()) {
            QuizQuestion question = questionRepository.findById(answerSubmit.getQuestionId())
                    .orElse(null);
            if (question == null) continue;

            Long correctOptionId = correctAnswers.get(answerSubmit.getQuestionId());
            boolean isCorrect = correctOptionId != null &&
                    correctOptionId.equals(answerSubmit.getSelectedOptionId());

            if (isCorrect) correctCount++;

            // Answer 저장
            QuizAnswer answer = QuizAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedOption(answerSubmit.getSelectedOptionId() != null ?
                            optionRepository.findById(answerSubmit.getSelectedOptionId()).orElse(null) : null)
                    .textAnswer(answerSubmit.getTextAnswer())
                    .isCorrect(isCorrect)
                    .build();
            answerRepository.save(answer);

            answerResults.add(QuizDto.AnswerResult.builder()
                    .questionId(answerSubmit.getQuestionId())
                    .isCorrect(isCorrect)
                    .correctOptionId(correctOptionId)
                    .selectedOptionId(answerSubmit.getSelectedOptionId())
                    .build());
        }

        // 점수 계산 및 합격 여부 결정
        attempt.setCorrectCount(correctCount);
        attempt.calculateScore();
        attempt.setPassed(attempt.getScorePercent() >= quiz.getPassingScore());
        attempt.setCompletedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        // 출석 처리 (퀴즈 제출 시 출석으로 인정)
        attendanceService.touchAttendance(userId);

        log.info("Quiz submitted: userId={}, quizId={}, score={}%, passed={}",
                userId, quiz.getId(), attempt.getScorePercent(), attempt.getPassed());

        return QuizDto.SubmitResult.builder()
                .attemptId(attempt.getId())
                .totalQuestions(attempt.getTotalQuestions())
                .correctCount(correctCount)
                .scorePercent(attempt.getScorePercent())
                .passingScore(quiz.getPassingScore())
                .passed(attempt.getPassed())
                .answerResults(answerResults)
                .build();
    }

    /**
     * 사용자의 강좌 퀴즈 최고 점수
     */
    public int getBestScoreForCourse(Long userId, Long courseId) {
        return attemptRepository.findBestScoreByUserIdAndCourseId(userId, courseId)
                .orElse(0);
    }

    /**
     * 사용자가 강좌 퀴즈를 통과했는지 확인
     */
    public boolean hasPassedCourseQuiz(Long userId, Long courseId) {
        return !attemptRepository.findPassedAttemptsByUserIdAndCourseId(userId, courseId).isEmpty();
    }

    /**
     * 강좌의 퀴즈 상태 조회
     */
    public Optional<QuizDto.QuizStatus> getQuizStatus(Long userId, Long courseId) {
        return quizRepository.findFirstByCourseIdAndActiveTrue(courseId)
                .map(quiz -> {
                    Integer bestScore = attemptRepository.findBestScoreByUserIdAndQuizId(userId, quiz.getId())
                            .orElse(null);
                    boolean hasPassed = attemptRepository.existsByUserIdAndQuizIdAndPassedTrue(userId, quiz.getId());
                    int attemptCount = attemptRepository.countByUserIdAndQuizId(userId, quiz.getId());

                    return QuizDto.QuizStatus.builder()
                            .quizId(quiz.getId())
                            .title(quiz.getTitle())
                            .bestScore(bestScore)
                            .hasPassed(hasPassed)
                            .attemptCount(attemptCount)
                            .build();
                });
    }

    /**
     * 사용자의 퀴즈 시도 기록
     */
    public List<QuizDto.AttemptHistory> getAttemptHistory(Long userId, Long quizId) {
        return attemptRepository.findByUserIdAndQuizIdOrderByStartedAtDesc(userId, quizId)
                .stream()
                .filter(QuizAttempt::isCompleted)
                .map(attempt -> QuizDto.AttemptHistory.builder()
                        .attemptId(attempt.getId())
                        .scorePercent(attempt.getScorePercent())
                        .passed(attempt.getPassed())
                        .completedAt(attempt.getCompletedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
