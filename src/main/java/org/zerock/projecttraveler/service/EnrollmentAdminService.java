package org.zerock.projecttraveler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.projecttraveler.entity.*;
import org.zerock.projecttraveler.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EnrollmentAdminService {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    /**
     * 승인 대기 목록 조회
     */
    public List<CourseEnrollment> findAllRequested() {
        return enrollmentRepository.findAllRequestedWithUserAndCourse();
    }

    /**
     * 수강 승인
     */
    @Transactional
    public CourseEnrollment approve(Long enrollmentId, String note) {
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("수강 신청을 찾을 수 없습니다."));

        if (enrollment.getStatus() != CourseEnrollment.Status.REQUESTED) {
            throw new IllegalArgumentException("승인 대기 상태의 신청만 승인할 수 있습니다.");
        }

        enrollment.setStatus(CourseEnrollment.Status.APPROVED);
        enrollment.setApprovedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            enrollment.setNote(note);
        }

        log.info("수강 승인: enrollmentId={}, userId={}, courseId={}",
                enrollmentId, enrollment.getUser().getId(), enrollment.getCourse().getId());

        return enrollment;
    }

    /**
     * 수강 반려
     */
    @Transactional
    public CourseEnrollment reject(Long enrollmentId, String note) {
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("수강 신청을 찾을 수 없습니다."));

        if (enrollment.getStatus() != CourseEnrollment.Status.REQUESTED) {
            throw new IllegalArgumentException("승인 대기 상태의 신청만 반려할 수 있습니다.");
        }

        enrollment.setStatus(CourseEnrollment.Status.REJECTED);
        if (note != null && !note.isBlank()) {
            enrollment.setNote(note);
        }

        log.info("수강 반려: enrollmentId={}, userId={}, courseId={}",
                enrollmentId, enrollment.getUser().getId(), enrollment.getCourse().getId());

        return enrollment;
    }

    /**
     * 관리자 배정 (필수/패키지 강의)
     */
    @Transactional
    public CourseEnrollment assign(Long adminId, Long userId, Long courseId, String note) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        // 이미 수강 중인지 확인
        CourseEnrollment existing = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElse(null);

        if (existing != null) {
            // 이미 신청 중이면 배정으로 승격
            if (existing.getStatus() == CourseEnrollment.Status.REQUESTED) {
                existing.setStatus(CourseEnrollment.Status.ASSIGNED);
                existing.setSource(CourseEnrollment.Source.ADMIN_ASSIGN);
                existing.setAssignedAt(LocalDateTime.now());
                existing.setAssignedByAdmin(admin);
                if (note != null && !note.isBlank()) {
                    existing.setNote(note);
                }
                return existing;
            }
            // 이미 수강 중이면 무시
            if (existing.isAccessible()) {
                return existing;
            }
            // 반려된 경우 배정으로 변경
            if (existing.getStatus() == CourseEnrollment.Status.REJECTED) {
                existing.setStatus(CourseEnrollment.Status.ASSIGNED);
                existing.setSource(CourseEnrollment.Source.ADMIN_ASSIGN);
                existing.setAssignedAt(LocalDateTime.now());
                existing.setAssignedByAdmin(admin);
                if (note != null && !note.isBlank()) {
                    existing.setNote(note);
                }
                return existing;
            }
        }

        // 새로 배정
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .user(user)
                .course(course)
                .status(CourseEnrollment.Status.ASSIGNED)
                .source(CourseEnrollment.Source.ADMIN_ASSIGN)
                .assignedAt(LocalDateTime.now())
                .assignedByAdmin(admin)
                .note(note)
                .build();

        log.info("관리자 배정: adminId={}, userId={}, courseId={}", adminId, userId, courseId);

        return enrollmentRepository.save(enrollment);
    }

    /**
     * 다수 사용자에게 배정
     */
    @Transactional
    public int assignToMultipleUsers(Long adminId, List<Long> userIds, Long courseId, String note) {
        int count = 0;
        for (Long userId : userIds) {
            try {
                assign(adminId, userId, courseId, note);
                count++;
            } catch (Exception e) {
                log.warn("배정 실패: userId={}, courseId={}, error={}", userId, courseId, e.getMessage());
            }
        }
        return count;
    }
}
