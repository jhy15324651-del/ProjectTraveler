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

    private static final int PASSING_SCORE = 80; // 합격 기준 점수
    private static final int MAX_ATTEMPTS_PER_CYCLE = 2; // 사이클당 최대 시도 횟수

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizOptionRepository optionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
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
     * 퀴즈 ID로 강좌 ID 조회
     */
    public Optional<Long> getCourseIdByQuizId(Long quizId) {
        return quizRepository.findById(quizId)
                .map(quiz -> quiz.getCourse().getId());
    }

    /**
     * 퀴즈 ID로 퀴즈 상태 조회
     */
    public Optional<QuizDto.QuizStatus> getQuizStatusByQuizId(Long userId, Long quizId) {
        Quiz quiz = quizRepository.findById(quizId).orElse(null);
        if (quiz == null) return Optional.empty();
        return getQuizStatus(userId, quiz.getCourse().getId());
    }

    /**
     * 퀴즈 응시 가능 여부 확인
     */
    public boolean canAttemptQuiz(Long userId, Long quizId) {
        Quiz quiz = quizRepository.findById(quizId).orElse(null);
        if (quiz == null) return false;

        CourseEnrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(userId, quiz.getCourse().getId())
                .orElse(null);
        if (enrollment == null) return false;

        // RETAKE_REQUIRED 상태면 응시 불가
        if (enrollment.getQuizStatus() == CourseEnrollment.QuizStatus.RETAKE_REQUIRED) {
            return false;
        }

        // 이미 합격한 경우 응시 불가 (중복 응시 방지)
        if (enrollment.getQuizStatus() == CourseEnrollment.QuizStatus.PASSED) {
            return false;
        }

        // 현재 사이클에서 시도 횟수 확인
        int currentCycle = enrollment.getQuizCycle();
        int attemptsInCycle = attemptRepository.countByUserIdAndQuizIdAndCycle(userId, quizId, currentCycle);

        return attemptsInCycle < MAX_ATTEMPTS_PER_CYCLE;
    }

    /**
     * 퀴즈 제출 및 채점 (핵심 로직)
     *
     * 규칙:
     * - 1차 시험: 80% 이상이면 PASS
     * - 1차 시험 80% 미만: RETRY_ALLOWED (정답/해설 공개, 2차 응시 가능)
     * - 2차 시험 80% 미만: RETAKE_REQUIRED (재수강 필요, 퀴즈 잠금)
     */
    @Transactional
    public QuizDto.SubmitResult submitQuiz(Long userId, QuizDto.SubmitRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Quiz quiz = quizRepository.findByIdWithQuestionsAndOptions(request.getQuizId())
                .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다."));

        // 수강 정보 조회
        CourseEnrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(userId, quiz.getCourse().getId())
                .orElseThrow(() -> new IllegalArgumentException("수강 정보를 찾을 수 없습니다."));

        int currentCycle = enrollment.getQuizCycle();

        // 응시 가능 여부 확인
        if (enrollment.getQuizStatus() == CourseEnrollment.QuizStatus.RETAKE_REQUIRED) {
            throw new IllegalStateException("재수강이 필요합니다. 강의를 다시 수강한 후 퀴즈에 응시해주세요.");
        }

        if (enrollment.getQuizStatus() == CourseEnrollment.QuizStatus.PASSED) {
            throw new IllegalStateException("이미 퀴즈를 통과했습니다.");
        }

        // 현재 사이클에서 시도 횟수 확인
        int attemptsInCycle = attemptRepository.countByUserIdAndQuizIdAndCycle(userId, quiz.getId(), currentCycle);
        if (attemptsInCycle >= MAX_ATTEMPTS_PER_CYCLE) {
            throw new IllegalStateException("이 사이클에서 최대 응시 횟수(2회)를 초과했습니다.");
        }

        int attemptNo = attemptsInCycle + 1;

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
                .attemptNo(attemptNo)
                .cycle(currentCycle)
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

        // 점수 계산
        attempt.setCorrectCount(correctCount);
        attempt.calculateScore();
        int scorePercent = attempt.getScorePercent();
        boolean passed = scorePercent >= PASSING_SCORE;
        attempt.setPassed(passed);
        attempt.setCompletedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        // 상태 결정 및 업데이트
        QuizDto.ResultStatus resultStatus;
        boolean showReview = false;

        if (passed) {
            // 합격
            resultStatus = QuizDto.ResultStatus.PASS;
            enrollment.setQuizStatus(CourseEnrollment.QuizStatus.PASSED);
            log.info("Quiz PASSED: userId={}, quizId={}, score={}%, attemptNo={}, cycle={}",
                    userId, quiz.getId(), scorePercent, attemptNo, currentCycle);
        } else if (attemptNo == 1) {
            // 1차 실패 -> 2차 응시 가능, 정답/해설 공개
            resultStatus = QuizDto.ResultStatus.RETRY_ALLOWED;
            showReview = true;
            enrollment.setQuizStatus(CourseEnrollment.QuizStatus.RETRY_ALLOWED);
            log.info("Quiz 1st attempt FAILED: userId={}, quizId={}, score={}%. Retry allowed.",
                    userId, quiz.getId(), scorePercent);
        } else {
            // 2차 실패 -> 재수강 필요, 레슨 진도 초기화
            resultStatus = QuizDto.ResultStatus.RETAKE_REQUIRED;
            showReview = true;
            enrollment.setQuizStatus(CourseEnrollment.QuizStatus.RETAKE_REQUIRED);

            // 레슨 진도를 0%로 초기화
            int resetCount = lessonProgressRepository.resetProgressByUserIdAndCourseId(userId, quiz.getCourse().getId());
            log.info("Quiz 2nd attempt FAILED: userId={}, quizId={}, score={}%. Retake required. Reset {} lesson progress records.",
                    userId, quiz.getId(), scorePercent, resetCount);
        }

        enrollmentRepository.save(enrollment);

        // 출석 처리 (퀴즈 제출 시 출석으로 인정)
        attendanceService.touchAttendance(userId);

        return QuizDto.SubmitResult.builder()
                .attemptId(attempt.getId())
                .attemptNo(attemptNo)
                .cycle(currentCycle)
                .totalQuestions(attempt.getTotalQuestions())
                .correctCount(correctCount)
                .scorePercent(scorePercent)
                .passingScore(PASSING_SCORE)
                .passed(passed)
                .status(resultStatus)
                .answerResults(answerResults)
                .showReview(showReview)
                .build();
    }

    /**
     * 정답/해설 조회 (1차 실패 후에만 가능)
     */
    public Optional<QuizDto.ReviewInfo> getReview(Long userId, Long quizId, Integer attemptNo) {
        Quiz quiz = quizRepository.findByIdWithQuestionsAndOptions(quizId).orElse(null);
        if (quiz == null) return Optional.empty();

        CourseEnrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(userId, quiz.getCourse().getId())
                .orElse(null);
        if (enrollment == null) return Optional.empty();

        int currentCycle = enrollment.getQuizCycle();

        // 해당 시도 조회
        QuizAttempt attempt = attemptRepository
                .findByUserIdAndQuizIdAndCycleAndAttemptNo(userId, quizId, currentCycle, attemptNo)
                .orElse(null);
        if (attempt == null) return Optional.empty();

        // 합격한 경우는 리뷰 불필요
        if (attempt.getPassed()) {
            return Optional.empty();
        }

        // 1차 실패 후에만 리뷰 가능 (또는 2차 실패 후)
        List<QuizAnswer> answers = answerRepository.findByAttemptId(attempt.getId());
        Map<Long, QuizAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a));

        List<QuizDto.QuestionReview> questionReviews = quiz.getQuestions().stream()
                .map(q -> {
                    QuizAnswer answer = answerMap.get(q.getId());
                    Long correctOptionId = q.getOptions().stream()
                            .filter(QuizOption::getIsCorrect)
                            .findFirst()
                            .map(QuizOption::getId)
                            .orElse(null);

                    return QuizDto.QuestionReview.builder()
                            .questionId(q.getId())
                            .question(q.getQuestion())
                            .options(q.getOptions().stream()
                                    .map(o -> QuizDto.OptionInfo.from(o, true))
                                    .collect(Collectors.toList()))
                            .correctOptionId(correctOptionId)
                            .selectedOptionId(answer != null && answer.getSelectedOption() != null ?
                                    answer.getSelectedOption().getId() : null)
                            .isCorrect(answer != null ? answer.getIsCorrect() : false)
                            .explanation(null) // 추후 해설 필드 추가 시 사용
                            .build();
                })
                .collect(Collectors.toList());

        return Optional.of(QuizDto.ReviewInfo.builder()
                .quizId(quizId)
                .attemptId(attempt.getId())
                .attemptNo(attemptNo)
                .cycle(currentCycle)
                .scorePercent(attempt.getScorePercent())
                .questions(questionReviews)
                .build());
    }

    /**
     * 재수강 시작 (RETAKE_REQUIRED 상태에서만)
     */
    @Transactional
    public boolean startRetake(Long userId, Long courseId) {
        CourseEnrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(userId, courseId)
                .orElse(null);

        if (enrollment == null) {
            throw new IllegalArgumentException("수강 정보를 찾을 수 없습니다.");
        }

        if (enrollment.getQuizStatus() != CourseEnrollment.QuizStatus.RETAKE_REQUIRED) {
            throw new IllegalStateException("재수강이 필요한 상태가 아닙니다.");
        }

        // 상태를 IN_PROGRESS로 변경 (아직 사이클은 증가하지 않음)
        // 재수강 완료 시점에 사이클 증가
        log.info("Retake started: userId={}, courseId={}", userId, courseId);
        return true;
    }

    /**
     * 재수강 완료 처리 (진도 조건 충족 시 호출)
     * 옵션 A: 새 사이클 시작, attempt 초기화
     */
    @Transactional
    public boolean completeRetake(Long userId, Long courseId) {
        CourseEnrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(userId, courseId)
                .orElse(null);

        if (enrollment == null) {
            throw new IllegalArgumentException("수강 정보를 찾을 수 없습니다.");
        }

        if (enrollment.getQuizStatus() != CourseEnrollment.QuizStatus.RETAKE_REQUIRED) {
            throw new IllegalStateException("재수강이 필요한 상태가 아닙니다.");
        }

        // 새 사이클 시작
        int newCycle = enrollment.getQuizCycle() + 1;
        enrollment.setQuizCycle(newCycle);
        enrollment.setQuizStatus(CourseEnrollment.QuizStatus.IN_PROGRESS);
        enrollmentRepository.save(enrollment);

        log.info("Retake completed: userId={}, courseId={}, newCycle={}", userId, courseId, newCycle);
        return true;
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
     * 강좌의 퀴즈 상태 조회 (상세)
     */
    public Optional<QuizDto.QuizStatus> getQuizStatus(Long userId, Long courseId) {
        return quizRepository.findFirstByCourseIdAndActiveTrue(courseId)
                .map(quiz -> {
                    CourseEnrollment enrollment = enrollmentRepository
                            .findByUserIdAndCourseId(userId, courseId)
                            .orElse(null);

                    int currentCycle = enrollment != null ? enrollment.getQuizCycle() : 1;
                    CourseEnrollment.QuizStatus quizStatus = enrollment != null ?
                            enrollment.getQuizStatus() : CourseEnrollment.QuizStatus.NOT_STARTED;

                    Integer bestScore = attemptRepository.findBestScoreByUserIdAndQuizId(userId, quiz.getId())
                            .orElse(null);
                    boolean hasPassed = attemptRepository.existsByUserIdAndQuizIdAndPassedTrue(userId, quiz.getId());
                    int attemptCount = attemptRepository.countByUserIdAndQuizId(userId, quiz.getId());
                    int attemptsInCycle = attemptRepository.countByUserIdAndQuizIdAndCycle(userId, quiz.getId(), currentCycle);

                    boolean canAttempt = quizStatus != CourseEnrollment.QuizStatus.RETAKE_REQUIRED
                            && quizStatus != CourseEnrollment.QuizStatus.PASSED
                            && attemptsInCycle < MAX_ATTEMPTS_PER_CYCLE;

                    String message = getStatusMessage(quizStatus, attemptsInCycle);

                    return QuizDto.QuizStatus.builder()
                            .quizId(quiz.getId())
                            .title(quiz.getTitle())
                            .passingScore(PASSING_SCORE)
                            .bestScore(bestScore)
                            .hasPassed(hasPassed)
                            .attemptCount(attemptCount)
                            .currentCycle(currentCycle)
                            .attemptsInCycle(attemptsInCycle)
                            .quizStatusCode(quizStatus.name())
                            .canAttempt(canAttempt)
                            .message(message)
                            .build();
                });
    }

    private String getStatusMessage(CourseEnrollment.QuizStatus status, int attemptsInCycle) {
        return switch (status) {
            case NOT_STARTED, IN_PROGRESS -> "퀴즈에 응시할 수 있습니다. (1차 시험)";
            case RETRY_ALLOWED -> "1차 시험 불합격. 2차 시험에 응시할 수 있습니다.";
            case PASSED -> "퀴즈를 통과했습니다.";
            case RETAKE_REQUIRED -> "2차 시험 불합격. 강의를 다시 수강한 후 퀴즈에 응시해주세요.";
        };
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
