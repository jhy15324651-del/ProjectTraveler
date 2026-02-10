package org.zerock.projecttraveler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.projecttraveler.entity.*;
import org.zerock.projecttraveler.repository.*;
import org.zerock.projecttraveler.security.SecurityUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseUnitRepository courseUnitRepository;
    private final LessonRepository lessonRepository;
    private final CourseDeletionLogRepository courseDeletionLogRepository;

    /**
     * 모든 활성 강좌 조회
     */
    public List<Course> findAllActiveCourses() {
        return courseRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    /**
     * 카테고리별 강좌 조회
     */
    public List<Course> findByCategory(Course.Category category) {
        return courseRepository.findByCategoryAndActiveTrueOrderByCreatedAtDesc(category);
    }

    /**
     * 강좌 상세 조회 (유닛 및 레슨 포함)
     */
    public Optional<Course> findByIdWithUnitsAndLessons(Long courseId) {
        return courseRepository.findByIdWithUnitsAndLessons(courseId);
    }

    /**
     * 강좌 조회
     */
    public Optional<Course> findById(Long courseId) {
        return courseRepository.findById(courseId);
    }

    /**
     * 강좌 생성 (관리자 전용)
     */
    @Transactional
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    /**
     * 강좌 업데이트 (관리자 전용)
     */
    @Transactional
    public Course updateCourse(Long courseId, Course updatedCourse) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        course.setTitle(updatedCourse.getTitle());
        course.setShortDesc(updatedCourse.getShortDesc());
        course.setFullDesc(updatedCourse.getFullDesc());
        course.setThumbnailUrl(updatedCourse.getThumbnailUrl());
        course.setCategory(updatedCourse.getCategory());
        course.setLevel(updatedCourse.getLevel());
        course.setEnrollPolicy(updatedCourse.getEnrollPolicy());

        return course;
    }

    /**
     * 유닛 생성
     */
    @Transactional
    public CourseUnit createUnit(Long courseId, String title, int sortOrder) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        CourseUnit unit = CourseUnit.builder()
                .course(course)
                .title(title)
                .sortOrder(sortOrder)
                .build();

        return courseUnitRepository.save(unit);
    }

    /**
     * 레슨 생성
     */
    @Transactional
    public Lesson createLesson(Long courseId, Long unitId, String title, int sortOrder, int durationSec) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        CourseUnit unit = null;
        if (unitId != null) {
            unit = courseUnitRepository.findById(unitId)
                    .orElseThrow(() -> new IllegalArgumentException("유닛을 찾을 수 없습니다."));
        }

        // sortOrder가 0 이하이면 기존 레슨 수 + 1로 자동 설정
        if (sortOrder <= 0) {
            List<Lesson> existing = lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId);
            sortOrder = existing.size() + 1;
        }

        Lesson lesson = Lesson.builder()
                .course(course)
                .unit(unit)
                .title(title)
                .sortOrder(sortOrder)
                .durationSec(durationSec)
                .videoType(Lesson.VideoType.NONE)
                .build();

        Lesson saved = lessonRepository.save(lesson);

        // 강좌 총 시간 업데이트
        updateCourseTotalDuration(courseId);

        return saved;
    }

    /**
     * 레슨 영상 URL 업데이트
     */
    @Transactional
    public Lesson updateLessonVideo(Long lessonId, Lesson.VideoType videoType, String videoUrl, Integer durationSec) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("레슨을 찾을 수 없습니다."));

        lesson.setVideoType(videoType);
        lesson.setVideoUrl(videoUrl);
        if (durationSec != null) {
            lesson.setDurationSec(durationSec);
        }

        // 강좌 총 시간 업데이트
        updateCourseTotalDuration(lesson.getCourse().getId());

        return lesson;
    }

    /**
     * 강좌 총 시간 재계산
     */
    @Transactional
    public void updateCourseTotalDuration(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId);
        int totalDuration = lessons.stream()
                .mapToInt(l -> l.getDurationSec() != null ? l.getDurationSec() : 0)
                .sum();

        course.setTotalDurationSec(totalDuration);
    }

    /**
     * 레슨 조회
     */
    public Optional<Lesson> findLessonById(Long lessonId) {
        return lessonRepository.findById(lessonId);
    }

    /**
     * 강좌의 첫 번째 레슨 조회
     */
    public Optional<Lesson> findFirstLesson(Long courseId) {
        return lessonRepository.findFirstLessonByCourseId(courseId);
    }

    /**
     * 다음 레슨 조회
     */
    public Optional<Lesson> findNextLesson(Long courseId, Integer currentSortOrder) {
        return lessonRepository.findNextLesson(courseId, currentSortOrder);
    }

    /**
     * 강좌의 모든 유닛 조회 (레슨 포함)
     */
    public List<CourseUnit> findUnitsWithLessons(Long courseId) {
        return courseUnitRepository.findByCourseIdWithLessons(courseId);
    }

    /**
     * 강좌의 모든 레슨 조회
     */
    public List<Lesson> findLessonsByCourseId(Long courseId) {
        return lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId);
    }

    /**
     * 총 레슨 수
     */
    public int countLessons(Long courseId) {
        return lessonRepository.countByCourseId(courseId);
    }

    /**
     * 강좌 삭제 (Soft Delete + 이력 저장)
     * @param courseId 삭제할 강좌 ID
     * @param reason 삭제 이유
     * @param deletedBy 삭제한 사람 이름
     * @param deletedByUserId 삭제한 사람 ID
     */
    @Transactional
    public void deleteCourse(Long courseId, String reason, String deletedBy, Long deletedByUserId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        // 삭제 이력 저장
        CourseDeletionLog deletionLog = CourseDeletionLog.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseCategory(course.getCategory())
                .courseLevel(course.getLevel())
                .lessonCount(course.getTotalLessonCount())
                .reason(reason)
                .deletedBy(deletedBy)
                .deletedByUserId(deletedByUserId)
                .courseCreatedAt(course.getCreatedAt())
                .build();

        courseDeletionLogRepository.save(deletionLog);

        log.info("강좌 삭제 이력 저장: courseId={}, title={}, deletedBy={}, reason={}",
                course.getId(), course.getTitle(), deletedBy, reason);

        // Soft Delete: active = false
        course.setActive(false);
    }

    /**
     * 삭제된 강좌 목록 조회
     */
    public List<Course> findAllDeletedCourses() {
        return courseRepository.findByActiveFalseOrderByUpdatedAtDesc();
    }

    /**
     * 강좌 삭제 이력 조회
     */
    public List<CourseDeletionLog> findDeletionLogs() {
        return courseDeletionLogRepository.findTop10ByOrderByDeletedAtDesc();
    }
}
