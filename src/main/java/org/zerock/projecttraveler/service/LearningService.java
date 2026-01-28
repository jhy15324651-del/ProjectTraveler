package org.zerock.projecttraveler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.projecttraveler.dto.CourseDetailDto;
import org.zerock.projecttraveler.entity.*;
import org.zerock.projecttraveler.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LearningService {

    private final CourseRepository courseRepository;
    private final CourseUnitRepository unitRepository;
    private final LessonRepository lessonRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository progressRepository;
    private final LearningSessionRepository sessionRepository;
    private final AttendanceService attendanceService;
    private final EnrollmentService enrollmentService;

    // 90% 이상 시청 시 완료로 처리
    private static final double COMPLETION_THRESHOLD = 0.9;

    /**
     * 강좌 상세 정보 (커리큘럼 + 진도 포함)
     */
    public CourseDetailDto getCourseDetailView(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        List<CourseUnit> units = unitRepository.findByCourseIdWithLessons(courseId);
        List<Lesson> allLessons = lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId);

        // 기본 정보
        CourseDetailDto dto = CourseDetailDto.from(course);
        dto.setTotalLessonCount(allLessons.size());
        dto.setTotalUnitCount(units.size());

        // 수강 정보
        CourseEnrollment enrollment = null;
        Map<Long, LessonProgress> progressMap = new HashMap<>();

        if (userId != null) {
            enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);

            List<LessonProgress> progressList = progressRepository.findByUserIdAndCourseIdWithLesson(userId, courseId);
            progressMap = progressList.stream()
                    .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p));
        }

        // 수강 정보 설정
        if (enrollment != null) {
            dto.setEnrollmentInfo(CourseDetailDto.EnrollmentInfo.builder()
                    .enrollmentId(enrollment.getId())
                    .status(enrollment.getStatus().name())
                    .statusDisplayName(enrollment.getStatusDisplayName())
                    .accessible(enrollment.isAccessible())
                    .enrollPolicy(course.getEnrollPolicy().name())
                    .lastAccessedAt(enrollment.getLastAccessedAt())
                    .build());
        } else {
            dto.setEnrollmentInfo(CourseDetailDto.EnrollmentInfo.builder()
                    .accessible(false)
                    .enrollPolicy(course.getEnrollPolicy().name())
                    .build());
        }

        // 진도 정보 설정
        int completedCount = (int) progressMap.values().stream().filter(LessonProgress::getCompleted).count();
        long totalWatchedSec = progressMap.values().stream().mapToLong(LessonProgress::getWatchedSec).sum();

        dto.setProgressInfo(CourseDetailDto.ProgressInfo.builder()
                .completedLessonCount(completedCount)
                .totalLessonCount(allLessons.size())
                .progressPercent(allLessons.isEmpty() ? 0 : (completedCount * 100 / allLessons.size()))
                .totalWatchedSec(totalWatchedSec)
                .totalWatchedFormatted(formatTime(totalWatchedSec))
                .build());

        // 유닛 및 레슨 정보 설정
        List<CourseDetailDto.UnitDto> unitDtos = new ArrayList<>();
        Lesson continueLesson = null;
        Lesson firstIncompleteLesson = null;

        // 유닛에 속하지 않은 레슨들 처리 (unit_id가 NULL인 레슨들)
        List<Lesson> unassignedLessons = allLessons.stream()
                .filter(l -> l.getUnit() == null)
                .collect(Collectors.toList());

        if (!unassignedLessons.isEmpty()) {
            List<CourseDetailDto.LessonDto> lessonDtos = new ArrayList<>();
            int unitCompletedCount = 0;
            boolean hasInProgress = false;

            for (Lesson lesson : unassignedLessons) {
                LessonProgress progress = progressMap.get(lesson.getId());

                String status = "미시작";
                int progressPercent = 0;
                int lastPositionSec = 0;
                boolean completed = false;

                if (progress != null) {
                    completed = progress.getCompleted();
                    lastPositionSec = progress.getLastPositionSec() != null ? progress.getLastPositionSec() : 0;
                    progressPercent = progress.getProgressPercent();

                    if (completed) {
                        status = "완료";
                        unitCompletedCount++;
                    } else if (progress.getWatchedSec() != null && progress.getWatchedSec() > 0) {
                        status = "진행중";
                        hasInProgress = true;
                        if (continueLesson == null) {
                            continueLesson = lesson;
                        }
                    }
                }

                if (!completed && firstIncompleteLesson == null) {
                    firstIncompleteLesson = lesson;
                }

                lessonDtos.add(CourseDetailDto.LessonDto.builder()
                        .id(lesson.getId())
                        .title(lesson.getTitle())
                        .sortOrder(lesson.getSortOrder())
                        .formattedDuration(lesson.getFormattedDuration())
                        .videoType(lesson.getVideoType().name())
                        .videoUrl(lesson.getVideoUrl())
                        .isPreview(lesson.getIsPreview())
                        .status(status)
                        .progressPercent(progressPercent)
                        .lastPositionSec(lastPositionSec)
                        .completed(completed)
                        .build());
            }

            // 유닛 상태 결정
            String unitStatus;
            if (unitCompletedCount == unassignedLessons.size()) {
                unitStatus = "완료";
            } else if (hasInProgress || unitCompletedCount > 0) {
                unitStatus = "진행중";
            } else {
                unitStatus = "미시작";
            }

            // 기본 유닛으로 추가
            unitDtos.add(CourseDetailDto.UnitDto.builder()
                    .id(0L)
                    .title("기본 커리큘럼")
                    .sortOrder(0)
                    .lessonCount(unassignedLessons.size())
                    .formattedDuration(formatLessonsTotalDuration(unassignedLessons))
                    .status(unitStatus)
                    .lessons(lessonDtos)
                    .build());
        }

        for (CourseUnit unit : units) {
            List<CourseDetailDto.LessonDto> lessonDtos = new ArrayList<>();
            int unitCompletedCount = 0;
            boolean hasInProgress = false;

            for (Lesson lesson : unit.getLessons()) {
                LessonProgress progress = progressMap.get(lesson.getId());

                String status = "미시작";
                int progressPercent = 0;
                int lastPositionSec = 0;
                boolean completed = false;

                if (progress != null) {
                    completed = progress.getCompleted();
                    lastPositionSec = progress.getLastPositionSec() != null ? progress.getLastPositionSec() : 0;
                    progressPercent = progress.getProgressPercent();

                    if (completed) {
                        status = "완료";
                        unitCompletedCount++;
                    } else if (progress.getWatchedSec() != null && progress.getWatchedSec() > 0) {
                        status = "진행중";
                        hasInProgress = true;
                        if (continueLesson == null) {
                            continueLesson = lesson;
                        }
                    }
                }

                // 첫 번째 미완료 레슨 찾기
                if (!completed && firstIncompleteLesson == null) {
                    firstIncompleteLesson = lesson;
                }

                lessonDtos.add(CourseDetailDto.LessonDto.builder()
                        .id(lesson.getId())
                        .title(lesson.getTitle())
                        .sortOrder(lesson.getSortOrder())
                        .formattedDuration(lesson.getFormattedDuration())
                        .videoType(lesson.getVideoType().name())
                        .videoUrl(lesson.getVideoUrl())
                        .isPreview(lesson.getIsPreview())
                        .status(status)
                        .progressPercent(progressPercent)
                        .lastPositionSec(lastPositionSec)
                        .completed(completed)
                        .build());
            }

            // 유닛 상태 결정
            String unitStatus;
            if (unitCompletedCount == unit.getLessons().size() && !unit.getLessons().isEmpty()) {
                unitStatus = "완료";
            } else if (hasInProgress || unitCompletedCount > 0) {
                unitStatus = "진행중";
            } else {
                unitStatus = "미시작";
            }

            unitDtos.add(CourseDetailDto.UnitDto.builder()
                    .id(unit.getId())
                    .title(unit.getTitle())
                    .sortOrder(unit.getSortOrder())
                    .lessonCount(unit.getLessons().size())
                    .formattedDuration(unit.getFormattedDuration())
                    .status(unitStatus)
                    .lessons(lessonDtos)
                    .build());
        }

        dto.setUnits(unitDtos);

        // 이어서 학습할 레슨 설정
        Lesson lessonToSet = continueLesson != null ? continueLesson :
                             (firstIncompleteLesson != null ? firstIncompleteLesson :
                             (enrollment != null && enrollment.getLastLesson() != null ? enrollment.getLastLesson() : null));

        if (lessonToSet != null) {
            LessonProgress progress = progressMap.get(lessonToSet.getId());
            dto.setContinueLesson(CourseDetailDto.LessonDto.builder()
                    .id(lessonToSet.getId())
                    .title(lessonToSet.getTitle())
                    .sortOrder(lessonToSet.getSortOrder())
                    .lastPositionSec(progress != null ? progress.getLastPositionSec() : 0)
                    .build());
        }

        return dto;
    }

    /**
     * Heartbeat 처리 (영상 학습 추적)
     */
    @Transactional
    public void heartbeat(Long userId, Long courseId, Long lessonId, int positionSec, int deltaWatchedSec) {
        // 수강 권한 확인
        CourseEnrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("수강 권한이 없습니다."));

        if (!enrollment.isAccessible()) {
            throw new IllegalArgumentException("수강 권한이 없습니다.");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("레슨을 찾을 수 없습니다."));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        // LessonProgress 업데이트 또는 생성
        LessonProgress progress = progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    LessonProgress newProgress = LessonProgress.builder()
                            .user(enrollment.getUser())
                            .course(course)
                            .lesson(lesson)
                            .lastPositionSec(0)
                            .watchedSec(0)
                            .completed(false)
                            .build();
                    return progressRepository.save(newProgress);
                });

        progress.setLastPositionSec(positionSec);
        progress.setWatchedSec(progress.getWatchedSec() + deltaWatchedSec);

        // Enrollment 업데이트
        enrollment.setLastAccessedAt(LocalDateTime.now());
        enrollment.setLastLesson(lesson);

        // 출석 처리
        try {
            attendanceService.touchAttendance(userId);
        } catch (Exception e) {
            log.warn("출석 처리 실패: userId={}", userId, e);
        }

        log.debug("Heartbeat: userId={}, lessonId={}, position={}, delta={}",
                userId, lessonId, positionSec, deltaWatchedSec);
    }

    /**
     * 레슨 완료 처리
     */
    @Transactional
    public boolean complete(Long userId, Long courseId, Long lessonId) {
        // 수강 권한 확인
        CourseEnrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("수강 권한이 없습니다."));

        if (!enrollment.isAccessible()) {
            throw new IllegalArgumentException("수강 권한이 없습니다.");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("레슨을 찾을 수 없습니다."));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        LessonProgress progress = progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    LessonProgress newProgress = LessonProgress.builder()
                            .user(enrollment.getUser())
                            .course(course)
                            .lesson(lesson)
                            .lastPositionSec(0)
                            .watchedSec(0)
                            .completed(false)
                            .build();
                    return progressRepository.save(newProgress);
                });

        // 90% 이상 시청 확인 (또는 영상이 없는 경우)
        if (lesson.getVideoType() == Lesson.VideoType.NONE ||
            lesson.getDurationSec() == null ||
            lesson.getDurationSec() == 0 ||
            (progress.getWatchedSec() >= lesson.getDurationSec() * COMPLETION_THRESHOLD)) {

            if (!progress.getCompleted()) {
                progress.setCompleted(true);
                progress.setCompletedAt(LocalDateTime.now());

                log.info("레슨 완료: userId={}, lessonId={}", userId, lessonId);

                // 강좌 완료 여부 체크
                enrollmentService.checkAndCompleteEnrollment(userId, courseId);
            }
            return true;
        }

        log.warn("완료 조건 미충족: userId={}, lessonId={}, watched={}, required={}",
                userId, lessonId, progress.getWatchedSec(),
                (int)(lesson.getDurationSec() * COMPLETION_THRESHOLD));
        return false;
    }

    /**
     * 레슨 진도 조회
     */
    public LessonProgress getLessonProgress(Long userId, Long lessonId) {
        return progressRepository.findByUserIdAndLessonId(userId, lessonId).orElse(null);
    }

    /**
     * 총 학습 시간 (초)
     */
    public long getTotalWatchedSec(Long userId) {
        return progressRepository.sumWatchedSecByUserId(userId);
    }

    /**
     * 특정 강좌의 레슨별 진도 목록 (레슨 정보 포함)
     */
    public List<LessonProgress> getProgressListByUserAndCourse(Long userId, Long courseId) {
        return progressRepository.findByUserIdAndCourseIdWithLesson(userId, courseId);
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0 && minutes > 0) {
            return String.format("%d시간 %d분", hours, minutes);
        } else if (hours > 0) {
            return String.format("%d시간", hours);
        } else {
            return String.format("%d분", minutes);
        }
    }

    private String formatLessonsTotalDuration(List<Lesson> lessons) {
        int totalSec = lessons.stream()
                .mapToInt(l -> l.getDurationSec() != null ? l.getDurationSec() : 0)
                .sum();
        if (totalSec == 0) return "미정";
        int minutes = totalSec / 60;
        if (minutes >= 60) {
            int hours = minutes / 60;
            int remainingMin = minutes % 60;
            return remainingMin > 0 ? String.format("약 %d시간 %d분", hours, remainingMin) : String.format("약 %d시간", hours);
        }
        return String.format("약 %d분", minutes);
    }
}
