package org.zerock.projecttraveler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.projecttraveler.dto.CertificateDto;
import org.zerock.projecttraveler.entity.Certificate;
import org.zerock.projecttraveler.entity.Course;
import org.zerock.projecttraveler.entity.User;
import org.zerock.projecttraveler.repository.CertificateRepository;
import org.zerock.projecttraveler.repository.CourseRepository;
import org.zerock.projecttraveler.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CertificateService {

    private static final int REQUIRED_PROGRESS_PERCENT = 90;
    private static final int REQUIRED_QUIZ_PERCENT = 90;

    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentService enrollmentService;
    private final QuizService quizService;

    /**
     * 사용자의 모든 수료증 조회
     */
    public List<CertificateDto.CertificateInfo> getMyCertificates(Long userId) {
        return certificateRepository.findByUserIdWithCourse(userId)
                .stream()
                .map(CertificateDto.CertificateInfo::from)
                .collect(Collectors.toList());
    }

    /**
     * 수료증 상세 조회
     */
    public Optional<CertificateDto.CertificateInfo> getCertificate(Long certificateId) {
        return certificateRepository.findById(certificateId)
                .map(CertificateDto.CertificateInfo::from);
    }

    /**
     * 수료증 번호로 조회
     */
    public Optional<CertificateDto.CertificateInfo> getCertificateByNumber(String certificateNumber) {
        return certificateRepository.findByCertificateNumber(certificateNumber)
                .map(CertificateDto.CertificateInfo::from);
    }

    /**
     * 수료증 발급 자격 확인
     */
    public CertificateDto.EligibilityCheck checkEligibility(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        // 이미 발급되었는지 확인
        boolean alreadyIssued = certificateRepository.existsByUserIdAndCourseId(userId, courseId);
        if (alreadyIssued) {
            return CertificateDto.EligibilityCheck.builder()
                    .courseId(courseId)
                    .courseTitle(course.getTitle())
                    .eligible(false)
                    .alreadyIssued(true)
                    .message("이미 수료증이 발급되었습니다.")
                    .build();
        }

        // 승인형 강의만 수료증 발급 가능
        if (course.getEnrollPolicy() != Course.EnrollPolicy.APPROVAL) {
            return CertificateDto.EligibilityCheck.builder()
                    .courseId(courseId)
                    .courseTitle(course.getTitle())
                    .eligible(false)
                    .alreadyIssued(false)
                    .message("승인형 강좌만 수료증 발급이 가능합니다.")
                    .build();
        }

        // 진도율 확인
        int progressPercent = enrollmentService.calculateProgressPercent(userId, courseId);

        // 퀴즈 점수 확인
        int quizPercent = quizService.getBestScoreForCourse(userId, courseId);
        boolean hasQuiz = quizService.hasQuiz(courseId);

        // 퀴즈가 없는 경우 퀴즈 점수를 100%로 간주
        if (!hasQuiz) {
            quizPercent = 100;
        }

        boolean progressMet = progressPercent >= REQUIRED_PROGRESS_PERCENT;
        boolean quizMet = quizPercent >= REQUIRED_QUIZ_PERCENT;
        boolean eligible = progressMet && quizMet;

        String message;
        if (eligible) {
            message = "수료증 발급 조건을 충족했습니다.";
        } else {
            StringBuilder sb = new StringBuilder("발급 조건 미충족: ");
            if (!progressMet) {
                sb.append(String.format("진도율 %d%% (필요: %d%%) ", progressPercent, REQUIRED_PROGRESS_PERCENT));
            }
            if (!quizMet && hasQuiz) {
                sb.append(String.format("퀴즈 %d%% (필요: %d%%)", quizPercent, REQUIRED_QUIZ_PERCENT));
            }
            message = sb.toString().trim();
        }

        return CertificateDto.EligibilityCheck.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .eligible(eligible)
                .progressPercent(progressPercent)
                .quizPercent(quizPercent)
                .requiredProgress(REQUIRED_PROGRESS_PERCENT)
                .requiredQuiz(REQUIRED_QUIZ_PERCENT)
                .message(message)
                .alreadyIssued(false)
                .build();
    }

    /**
     * 수료증 발급
     */
    @Transactional
    public CertificateDto.IssueResult issueCertificate(Long userId, Long courseId) {
        // 자격 확인
        CertificateDto.EligibilityCheck eligibility = checkEligibility(userId, courseId);

        if (eligibility.getAlreadyIssued()) {
            Certificate existing = certificateRepository.findByUserIdAndCourseId(userId, courseId)
                    .orElse(null);
            return CertificateDto.IssueResult.builder()
                    .success(false)
                    .certificateNumber(existing != null ? existing.getCertificateNumber() : null)
                    .message("이미 수료증이 발급되었습니다.")
                    .build();
        }

        if (!eligibility.getEligible()) {
            return CertificateDto.IssueResult.builder()
                    .success(false)
                    .message(eligibility.getMessage())
                    .build();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        // 수료증 생성
        Certificate certificate = Certificate.builder()
                .user(user)
                .course(course)
                .progressPercent(eligibility.getProgressPercent())
                .quizPercent(eligibility.getQuizPercent())
                .issuedAt(LocalDateTime.now())
                .build();

        certificate = certificateRepository.save(certificate);

        log.info("Certificate issued: userId={}, courseId={}, certificateNumber={}",
                userId, courseId, certificate.getCertificateNumber());

        return CertificateDto.IssueResult.builder()
                .success(true)
                .certificateNumber(certificate.getCertificateNumber())
                .message("수료증이 성공적으로 발급되었습니다.")
                .build();
    }

    /**
     * 사용자의 수료증 개수
     */
    public int getCertificateCount(Long userId) {
        return certificateRepository.countByUserId(userId);
    }
}
