package org.zerock.projecttraveler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.projecttraveler.dto.EnrollmentDto;
import org.zerock.projecttraveler.entity.*;
import org.zerock.projecttraveler.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EnrollmentService {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LessonProgressRepository progressRepository;
    private final LessonRepository lessonRepository;

    /**
     * 수강 신청 (사용자)
     */
    @Transactional
    public CourseEnrollment requestEnrollment(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        // 이미 수강 신청/수강 중인지 확인
        Optional<CourseEnrollment> existing = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        if (existing.isPresent()) {
            CourseEnrollment e = existing.get();
            if (e.getStatus() == CourseEnrollment.Status.REJECTED) {
                // 반려된 경우 재신청 가능
                e.setStatus(CourseEnrollment.Status.REQUESTED);
                e.setRequestedAt(LocalDateTime.now());
                return e;
            }
            throw new IllegalArgumentException("이미 수강 신청한 강좌입니다.");
        }

        // 수강 정책에 따른 처리
        CourseEnrollment.Status status;
        switch (course.getEnrollPolicy()) {
            case SELF:
                // 즉시 승인
                status = CourseEnrollment.Status.APPROVED;
                break;
            case APPROVAL:
                // 승인 대기
                status = CourseEnrollment.Status.REQUESTED;
                break;
            case ASSIGN_ONLY:
                throw new IllegalArgumentException("이 강좌는 관리자 배정만 가능합니다.");
            default:
                status = CourseEnrollment.Status.REQUESTED;
        }

        CourseEnrollment enrollment = CourseEnrollment.builder()
                .user(user)
                .course(course)
                .status(status)
                .source(CourseEnrollment.Source.USER_REQUEST)
                .requestedAt(LocalDateTime.now())
                .build();

        if (status == CourseEnrollment.Status.APPROVED) {
            enrollment.setApprovedAt(LocalDateTime.now());
        }

        return enrollmentRepository.save(enrollment);
    }

    /**
     * 수강 조회
     */
    public Optional<CourseEnrollment> findEnrollment(Long userId, Long courseId) {
        return enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
    }

    /**
     * 사용자의 학습 가능한 수강 목록
     */
    public List<CourseEnrollment> findAccessibleEnrollments(Long userId) {
        return enrollmentRepository.findAccessibleByUserId(userId);
    }

    /**
     * 사용자의 진행 중인 수강 목록
     */
    public List<CourseEnrollment> findInProgressEnrollments(Long userId) {
        return enrollmentRepository.findInProgressByUserId(userId);
    }

    /**
     * 사용자의 완료된 수강 목록
     */
    public List<CourseEnrollment> findCompletedEnrollments(Long userId) {
        return enrollmentRepository.findCompletedByUserId(userId);
    }

    /**
     * 사용자의 승인 대기 중인 수강 목록
     */
    public List<CourseEnrollment> findPendingEnrollments(Long userId) {
        return enrollmentRepository.findPendingByUserId(userId);
    }

    /**
     * 사용자의 수강 중인 강좌 수
     */
    public long countInProgressCourses(Long userId) {
        return enrollmentRepository.countInProgressByUserId(userId);
    }

    /**
     * 사용자의 완료된 강좌 수
     */
    public long countCompletedCourses(Long userId) {
        return enrollmentRepository.countCompletedByUserId(userId);
    }

    /**
     * 마지막 접근 시간 업데이트
     */
    @Transactional
    public void updateLastAccessed(Long userId, Long courseId, Long lessonId) {
        CourseEnrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElse(null);

        if (enrollment != null && enrollment.isAccessible()) {
            enrollment.setLastAccessedAt(LocalDateTime.now());
            if (lessonId != null) {
                lessonRepository.findById(lessonId).ifPresent(enrollment::setLastLesson);
            }
        }
    }

    /**
     * 진도율 계산
     */
    public int calculateProgressPercent(Long userId, Long courseId) {
        int totalLessons = lessonRepository.countByCourseId(courseId);
        if (totalLessons == 0) return 0;

        long completedLessons = progressRepository.countCompletedByUserIdAndCourseId(userId, courseId);
        return (int) ((completedLessons * 100) / totalLessons);
    }

    /**
     * 강좌 완료 체크 및 처리
     */
    @Transactional
    public void checkAndCompleteEnrollment(Long userId, Long courseId) {
        int progressPercent = calculateProgressPercent(userId, courseId);

        if (progressPercent >= 100) {
            enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                    .ifPresent(enrollment -> {
                        if (enrollment.getStatus() != CourseEnrollment.Status.COMPLETED) {
                            enrollment.setStatus(CourseEnrollment.Status.COMPLETED);
                            log.info("강좌 완료: userId={}, courseId={}", userId, courseId);
                        }
                    });
        }
    }

    /**
     * 수강 DTO로 변환 (진도 정보 포함)
     */
    public EnrollmentDto toEnrollmentDtoWithProgress(CourseEnrollment enrollment) {
        Long userId = enrollment.getUser().getId();
        Long courseId = enrollment.getCourse().getId();

        int totalLessons = lessonRepository.countByCourseId(courseId);
        long completedLessons = progressRepository.countCompletedByUserIdAndCourseId(userId, courseId);
        long watchedSec = progressRepository.sumWatchedSecByUserIdAndCourseId(userId, courseId);

        EnrollmentDto dto = EnrollmentDto.from(enrollment);
        dto.setTotalLessonCount(totalLessons);
        dto.setCompletedLessonCount((int) completedLessons);
        dto.setProgressPercent(totalLessons > 0 ? (int) ((completedLessons * 100) / totalLessons) : 0);
        dto.setTotalWatchedSec(watchedSec);
        dto.setTotalWatchedFormatted(formatTime(watchedSec));

        return dto;
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return String.format("%d시간 %d분", hours, minutes);
        } else {
            return String.format("%d분", minutes);
        }
    }
}
