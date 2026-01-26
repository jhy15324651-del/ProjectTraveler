package org.zerock.projecttraveler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.projecttraveler.dto.MyLearningSummaryDto;
import org.zerock.projecttraveler.entity.CourseEnrollment;
import org.zerock.projecttraveler.repository.*;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final AttendanceService attendanceService;
    private final LearningService learningService;

    /**
     * 나의 학습 요약 정보 조회 (메인, 마이페이지에서 사용)
     */
    public MyLearningSummaryDto getMyLearningSummary(Long userId) {
        // 수강 중인 강좌 수
        long inProgressCount = enrollmentRepository.countInProgressByUserId(userId);

        // 완료된 강좌 수
        long completedCount = enrollmentRepository.countCompletedByUserId(userId);

        // 평균 진도율 계산
        int averageProgress = calculateAverageProgress(userId);

        // 출석 정보
        int consecutiveDays = attendanceService.getConsecutiveAttendanceDays(userId);
        long totalAttendanceDays = attendanceService.getTotalAttendanceDays(userId);
        long thisMonthAttendance = attendanceService.getThisMonthAttendanceDays(userId);
        int attendanceRate = attendanceService.getAttendanceRate(userId);

        // 총 학습 시간
        long totalWatchedSec = learningService.getTotalWatchedSec(userId);
        String formattedTime = MyLearningSummaryDto.formatLearningTime(totalWatchedSec);

        return MyLearningSummaryDto.builder()
                .inProgressCourseCount(inProgressCount)
                .completedCourseCount(completedCount)
                .averageProgressPercent(averageProgress)
                .consecutiveAttendanceDays(consecutiveDays)
                .totalLearningTimeSec(totalWatchedSec)
                .totalLearningTimeFormatted(formattedTime)
                .totalAttendanceDays(totalAttendanceDays)
                .thisMonthAttendanceDays(thisMonthAttendance)
                .attendanceRate(attendanceRate)
                .build();
    }

    /**
     * 평균 진도율 계산
     */
    private int calculateAverageProgress(Long userId) {
        List<CourseEnrollment> enrollments = enrollmentRepository.findInProgressByUserId(userId);

        if (enrollments.isEmpty()) {
            return 0;
        }

        int totalProgress = 0;
        for (CourseEnrollment enrollment : enrollments) {
            Long courseId = enrollment.getCourse().getId();
            int totalLessons = lessonRepository.countByCourseId(courseId);
            if (totalLessons == 0) continue;

            long completedLessons = progressRepository.countCompletedByUserIdAndCourseId(userId, courseId);
            totalProgress += (int) ((completedLessons * 100) / totalLessons);
        }

        return totalProgress / enrollments.size();
    }
}
