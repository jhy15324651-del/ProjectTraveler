package org.zerock.projecttraveler.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.zerock.projecttraveler.dto.CourseDetailDto;
import org.zerock.projecttraveler.dto.EnrollmentDto;
import org.zerock.projecttraveler.dto.MyLearningSummaryDto;
import org.zerock.projecttraveler.entity.Course;
import org.zerock.projecttraveler.entity.CourseEnrollment;
import org.zerock.projecttraveler.entity.Lesson;
import org.zerock.projecttraveler.security.CustomUserDetails;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MainPageController {

    private final DashboardService dashboardService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final LearningService learningService;
    private final AttendanceService attendanceService;

    /**
     * 메인 페이지
     */
    @GetMapping("/main")
    public String main(Model model) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);

        // 학습 요약 정보
        MyLearningSummaryDto summary = dashboardService.getMyLearningSummary(userId);

        model.addAttribute("activePage", "main");
        model.addAttribute("username", user != null ? user.getFullName() : "사용자");
        model.addAttribute("isAdmin", SecurityUtils.isAdmin());
        model.addAttribute("summary", summary);

        return "main";
    }

    /**
     * 학습하기 (강좌 목록)
     */
    @GetMapping("/learning")
    public String learning(Model model) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);

        // 모든 강좌 조회
        List<Course> courses = courseService.findAllActiveCourses();

        // 각 강좌별 진도율 계산
        List<CourseWithProgress> coursesWithProgress = courses.stream()
                .map(course -> {
                    int progress = enrollmentService.calculateProgressPercent(userId, course.getId());
                    CourseEnrollment enrollment = enrollmentService.findEnrollment(userId, course.getId()).orElse(null);
                    return new CourseWithProgress(course, progress, enrollment);
                })
                .collect(Collectors.toList());

        model.addAttribute("activePage", "learning");
        model.addAttribute("username", user != null ? user.getFullName() : "사용자");
        model.addAttribute("isAdmin", SecurityUtils.isAdmin());
        model.addAttribute("courses", coursesWithProgress);

        return "learning";
    }

    /**
     * 강좌 상세
     */
    @GetMapping("/course-detail")
    public String courseDetail(@RequestParam("id") Long courseId, Model model) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);

        // 강좌 상세 정보 (커리큘럼 + 진도 포함)
        CourseDetailDto courseDetail = learningService.getCourseDetailView(courseId, userId);

        model.addAttribute("activePage", "course-detail");
        model.addAttribute("username", user != null ? user.getFullName() : "사용자");
        model.addAttribute("isAdmin", SecurityUtils.isAdmin());
        model.addAttribute("course", courseDetail);

        return "course-detail";
    }

    /**
     * 나의 강의실
     */
    @GetMapping("/my-classroom")
    public String myClassroom(Model model) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);

        // 학습 요약 정보
        MyLearningSummaryDto summary = dashboardService.getMyLearningSummary(userId);

        // 수강 중인 강좌 (진도 정보 포함)
        List<CourseEnrollment> inProgressEnrollments = enrollmentService.findInProgressEnrollments(userId);
        List<EnrollmentDto> inProgress = inProgressEnrollments.stream()
                .map(enrollmentService::toEnrollmentDtoWithProgress)
                .collect(Collectors.toList());

        // 완료된 강좌
        List<CourseEnrollment> completedEnrollments = enrollmentService.findCompletedEnrollments(userId);
        List<EnrollmentDto> completed = completedEnrollments.stream()
                .map(enrollmentService::toEnrollmentDtoWithProgress)
                .collect(Collectors.toList());

        // 승인 대기 중인 강좌
        List<CourseEnrollment> pendingEnrollments = enrollmentService.findPendingEnrollments(userId);
        List<EnrollmentDto> pending = pendingEnrollments.stream()
                .map(EnrollmentDto::from)
                .collect(Collectors.toList());

        model.addAttribute("activePage", "myClassroom");
        model.addAttribute("username", user != null ? user.getFullName() : "사용자");
        model.addAttribute("isAdmin", SecurityUtils.isAdmin());
        model.addAttribute("summary", summary);
        model.addAttribute("inProgressCourses", inProgress);
        model.addAttribute("completedCourses", completed);
        model.addAttribute("pendingCourses", pending);

        return "my-classroom";
    }

    /**
     * 출석 체크 페이지
     */
    @GetMapping("/attendance")
    public String attendance(Model model) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);

        // 출석 통계
        var stats = attendanceService.getStats(userId);

        // 이번 달 출석 현황
        var monthlyView = attendanceService.getMonthlyView(userId,
                java.time.LocalDate.now().getYear(),
                java.time.LocalDate.now().getMonthValue());

        // 최근 출석 기록
        var recentHistory = attendanceService.getRecentHistory(userId, 30);

        model.addAttribute("activePage", "attendance");
        model.addAttribute("username", user != null ? user.getFullName() : "사용자");
        model.addAttribute("isAdmin", SecurityUtils.isAdmin());
        model.addAttribute("stats", stats);
        model.addAttribute("monthlyView", monthlyView);
        model.addAttribute("recentHistory", recentHistory);

        return "attendance";
    }

    /**
     * 온라인 학습
     */
    @GetMapping("/online-learning")
    public String onlineLearning(Model model) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);

        // 모든 강좌 조회
        List<Course> courses = courseService.findAllActiveCourses();

        model.addAttribute("activePage", "online-learning");
        model.addAttribute("username", user != null ? user.getFullName() : "사용자");
        model.addAttribute("isAdmin", SecurityUtils.isAdmin());
        model.addAttribute("courses", courses);

        return "online-learning";
    }

    /**
     * 선택 페이지
     */
    @GetMapping("/select")
    public String select(Model model) {
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);

        model.addAttribute("activePage", "select");
        model.addAttribute("username", user != null ? user.getFullName() : "사용자");
        model.addAttribute("isAdmin", SecurityUtils.isAdmin());

        return "select";
    }

    /**
     * 이용안내
     */
    @GetMapping("/guide")
    public String guide(Model model) {
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);

        model.addAttribute("activePage", "guide");
        model.addAttribute("username", user != null ? user.getFullName() : "사용자");
        model.addAttribute("isAdmin", SecurityUtils.isAdmin());

        return "guide";
    }


    /**
     * 리뷰 페이지
     */
    @GetMapping("/reviews")
    public String reviews(Model model) {
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);

        model.addAttribute("activePage", "reviews");
        model.addAttribute("username", user != null ? user.getFullName() : "사용자");
        model.addAttribute("isAdmin", SecurityUtils.isAdmin());

        return "reviews";
    }

    /**
     * 레슨 시청 페이지
     */
    @GetMapping("/lesson")
    public String lesson(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long lessonId,
            Model model) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);

        // 강좌 조회
        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        // 수강 정보 조회
        CourseEnrollment enrollment = enrollmentService.findEnrollment(userId, courseId).orElse(null);
        boolean canAccess = enrollment != null && enrollment.isAccessible();

        // 레슨 목록
        List<Lesson> allLessons = courseService.findLessonsByCourseId(courseId);

        // 레슨 결정 (지정된 레슨 또는 첫 번째 레슨)
        Lesson lesson;
        if (lessonId != null) {
            lesson = courseService.findLessonById(lessonId)
                    .orElseThrow(() -> new IllegalArgumentException("레슨을 찾을 수 없습니다."));
        } else {
            lesson = courseService.findFirstLesson(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("등록된 레슨이 없습니다."));
        }

        // 진도 맵 (레슨별 완료 여부)
        java.util.Map<Long, org.zerock.projecttraveler.entity.LessonProgress> progressMap = new java.util.HashMap<>();
        if (canAccess) {
            List<org.zerock.projecttraveler.entity.LessonProgress> progressList =
                    learningService.getProgressListByUserAndCourse(userId, courseId);
            for (var p : progressList) {
                progressMap.put(p.getLesson().getId(), p);
            }
        }

        // YouTube URL → embed URL 변환
        String youtubeEmbedUrl = null;
        if (lesson.getVideoType() == Lesson.VideoType.YOUTUBE && lesson.getVideoUrl() != null) {
            youtubeEmbedUrl = convertToYoutubeEmbed(lesson.getVideoUrl());
        }

        model.addAttribute("activePage", "lesson");
        model.addAttribute("username", user != null ? user.getFullName() : "사용자");
        model.addAttribute("isAdmin", SecurityUtils.isAdmin());
        model.addAttribute("course", course);
        model.addAttribute("lesson", lesson);
        model.addAttribute("allLessons", allLessons);
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("canAccess", canAccess);
        model.addAttribute("progressMap", progressMap);
        model.addAttribute("youtubeEmbedUrl", youtubeEmbedUrl);

        return "lesson";
    }

    /**
     * YouTube URL을 embed URL로 변환
     */
    private String convertToYoutubeEmbed(String url) {
        if (url == null) return null;

        String videoId = null;

        // youtube.com/watch?v=VIDEO_ID
        if (url.contains("youtube.com/watch")) {
            int idx = url.indexOf("v=");
            if (idx != -1) {
                videoId = url.substring(idx + 2);
                int ampIdx = videoId.indexOf("&");
                if (ampIdx != -1) {
                    videoId = videoId.substring(0, ampIdx);
                }
            }
        }
        // youtu.be/VIDEO_ID
        else if (url.contains("youtu.be/")) {
            int idx = url.indexOf("youtu.be/");
            videoId = url.substring(idx + 9);
            int queryIdx = videoId.indexOf("?");
            if (queryIdx != -1) {
                videoId = videoId.substring(0, queryIdx);
            }
        }
        // 이미 embed URL인 경우
        else if (url.contains("youtube.com/embed/")) {
            return url;
        }

        if (videoId != null) {
            return "https://www.youtube.com/embed/" + videoId + "?enablejsapi=1";
        }

        return url;
    }

    /**
     * 강좌 + 진도 정보를 담는 내부 클래스
     */
    public record CourseWithProgress(Course course, int progressPercent, CourseEnrollment enrollment) {}
}
