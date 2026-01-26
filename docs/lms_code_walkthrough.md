# LMS 코드 상세 설명 (Code Walkthrough)

## 1. 패키지 구조

```
src/main/java/org/zerock/projecttraveler/
├── config/                 # 설정 클래스
│   ├── SecurityConfig.java        # Spring Security 설정
│   ├── WebConfig.java            # 정적 리소스 설정
│   └── DataInitializer.java      # 초기 데이터 생성
│
├── entity/                 # JPA 엔티티
│   ├── User.java                 # 사용자
│   ├── Course.java               # 강좌
│   ├── CourseUnit.java           # 유닛(단원)
│   ├── Lesson.java               # 레슨(강의)
│   ├── CourseEnrollment.java     # 수강 등록
│   ├── LessonProgress.java       # 레슨 진도
│   ├── LearningSession.java      # 학습 세션
│   └── AttendanceDaily.java      # 일일 출석
│
├── repository/             # JPA 리포지토리
│   ├── UserRepository.java
│   ├── CourseRepository.java
│   ├── CourseUnitRepository.java
│   ├── LessonRepository.java
│   ├── CourseEnrollmentRepository.java
│   ├── LessonProgressRepository.java
│   ├── LearningSessionRepository.java
│   └── AttendanceDailyRepository.java
│
├── service/                # 비즈니스 로직
│   ├── UserService.java          # 회원 관리
│   ├── CourseService.java        # 강좌 관리
│   ├── EnrollmentService.java    # 수강 관리 (사용자)
│   ├── EnrollmentAdminService.java # 수강 관리 (관리자)
│   ├── LearningService.java      # 학습/진도 관리
│   ├── AttendanceService.java    # 출석 관리
│   └── DashboardService.java     # 대시보드 집계
│
├── controller/             # 컨트롤러
│   ├── AuthController.java       # 인증 (로그인/회원가입)
│   ├── MainPageController.java   # 페이지 렌더링
│   ├── api/                      # REST API
│   │   ├── EnrollmentApiController.java
│   │   ├── LearningApiController.java
│   │   ├── AttendanceApiController.java
│   │   └── DashboardApiController.java
│   └── admin/                    # 관리자 전용
│       ├── AdminEnrollmentController.java
│       └── AdminCourseController.java
│
├── dto/                    # 데이터 전송 객체
│   ├── ApiResponse.java          # API 응답 래퍼
│   ├── CourseDetailDto.java      # 강좌 상세 DTO
│   ├── EnrollmentDto.java        # 수강 DTO
│   ├── MyLearningSummaryDto.java # 대시보드 DTO
│   ├── AttendanceDto.java        # 출석 DTO
│   ├── LearningHeartbeatRequest.java
│   └── LearningCompleteRequest.java
│
└── security/               # 보안 관련
    ├── CustomUserDetailsService.java
    ├── CustomUserDetails.java
    └── SecurityUtils.java        # userId 추출 유틸리티
```

---

## 2. 핵심 로직 상세 설명

### 2.1 Enrollment 상태 흐름

```java
// CourseEnrollment.java
public enum Status {
    REQUESTED,   // 신청됨 (승인 대기)
    APPROVED,    // 승인됨
    REJECTED,    // 반려됨
    ASSIGNED,    // 관리자 배정
    COMPLETED,   // 수강 완료
    PAUSED       // 일시 정지
}
```

**상태 전이 규칙:**
```
[신규 신청]
  └─ SELF 정책      → APPROVED (즉시)
  └─ APPROVAL 정책  → REQUESTED → APPROVED/REJECTED
  └─ ASSIGN_ONLY    → 신청 불가 (403)

[관리자 배정]
  └─ 신규 또는 REJECTED → ASSIGNED

[학습 완료]
  └─ APPROVED/ASSIGNED → COMPLETED (전체 레슨 완료 시)
```

**권한 체크 (isAccessible):**
```java
public boolean isAccessible() {
    return status == Status.APPROVED ||
           status == Status.ASSIGNED ||
           status == Status.COMPLETED ||
           status == Status.PAUSED;
}
```

### 2.2 Heartbeat 처리 로직

**LearningService.heartbeat() 흐름:**

```java
// 1. 수강 권한 확인
CourseEnrollment enrollment = enrollmentRepository
    .findByUserIdAndCourseId(userId, courseId)
    .orElseThrow(() -> new IllegalArgumentException("수강 권한이 없습니다."));

if (!enrollment.isAccessible()) {
    throw new IllegalArgumentException("수강 권한이 없습니다.");
}

// 2. LessonProgress 업데이트/생성
LessonProgress progress = progressRepository
    .findByUserIdAndLessonId(userId, lessonId)
    .orElseGet(() -> createNewProgress(...));

progress.setLastPositionSec(positionSec);  // 마지막 재생 위치
progress.setWatchedSec(progress.getWatchedSec() + deltaWatchedSec);  // 누적 시청

// 3. Enrollment 마지막 접근 정보 업데이트
enrollment.setLastAccessedAt(LocalDateTime.now());
enrollment.setLastLesson(lesson);

// 4. 출석 처리 (하루 1회)
attendanceService.touchAttendance(userId);
```

### 2.3 레슨 완료 처리

**LearningService.complete() 로직:**

```java
// 90% 이상 시청 시 완료로 처리
private static final double COMPLETION_THRESHOLD = 0.9;

// 완료 조건 검증
if (lesson.getVideoType() == VideoType.NONE ||  // 영상 없음
    lesson.getDurationSec() == 0 ||              // 시간 미설정
    (progress.getWatchedSec() >= lesson.getDurationSec() * COMPLETION_THRESHOLD)) {

    progress.setCompleted(true);
    progress.setCompletedAt(LocalDateTime.now());

    // 강좌 전체 완료 체크
    enrollmentService.checkAndCompleteEnrollment(userId, courseId);
}
```

### 2.4 진도율 계산

```java
// EnrollmentService.calculateProgressPercent()
public int calculateProgressPercent(Long userId, Long courseId) {
    int totalLessons = lessonRepository.countByCourseId(courseId);
    if (totalLessons == 0) return 0;

    long completedLessons = progressRepository
        .countCompletedByUserIdAndCourseId(userId, courseId);

    return (int) ((completedLessons * 100) / totalLessons);
}
```

### 2.5 연속 출석일 계산

```java
// AttendanceService.getConsecutiveAttendanceDays()
public int getConsecutiveAttendanceDays(Long userId) {
    List<LocalDate> dates = attendanceRepository.findAttendDatesByUserId(userId);

    if (dates.isEmpty()) return 0;

    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);

    // 오늘이나 어제 출석이 없으면 연속 끊김
    if (!dates.contains(today) && !dates.contains(yesterday)) {
        return 0;
    }

    // 연속 카운트
    int consecutive = 0;
    LocalDate checkDate = dates.contains(today) ? today : yesterday;

    for (LocalDate date : dates) {
        if (date.equals(checkDate)) {
            consecutive++;
            checkDate = checkDate.minusDays(1);
        } else if (date.isBefore(checkDate)) {
            break;
        }
    }

    return consecutive;
}
```

### 2.6 대시보드 집계

```java
// DashboardService.getMyLearningSummary()
public MyLearningSummaryDto getMyLearningSummary(Long userId) {
    return MyLearningSummaryDto.builder()
        .inProgressCourseCount(enrollmentRepository.countInProgressByUserId(userId))
        .completedCourseCount(enrollmentRepository.countCompletedByUserId(userId))
        .averageProgressPercent(calculateAverageProgress(userId))
        .consecutiveAttendanceDays(attendanceService.getConsecutiveAttendanceDays(userId))
        .totalLearningTimeSec(learningService.getTotalWatchedSec(userId))
        .totalLearningTimeFormatted(formatLearningTime(...))
        .totalAttendanceDays(attendanceService.getTotalAttendanceDays(userId))
        .thisMonthAttendanceDays(attendanceService.getThisMonthAttendanceDays(userId))
        .attendanceRate(attendanceService.getAttendanceRate(userId))
        .build();
}
```

---

## 3. 보안 관련 코드

### 3.1 SecurityConfig 주요 설정

```java
.authorizeHttpRequests(auth -> auth
    // 정적 리소스 허용
    .requestMatchers("/css/**", "/js/**").permitAll()
    // 인증 페이지 허용
    .requestMatchers("/", "/login", "/register").permitAll()
    // 관리자 전용
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    // 학습 API는 로그인 필요
    .requestMatchers("/api/learning/**").authenticated()
    // 나머지는 인증 필요
    .anyRequest().authenticated()
)
```

### 3.2 userId 서버 강제 추출

```java
// SecurityUtils.java
public static Long getCurrentUserIdOrThrow() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
        return ((CustomUserDetails) auth.getPrincipal()).getId();
    }
    throw new IllegalStateException("로그인이 필요합니다.");
}

// 사용 예시 (LearningApiController)
@PostMapping("/heartbeat")
public ResponseEntity<?> heartbeat(@RequestBody LearningHeartbeatRequest request) {
    // userId를 클라이언트가 아닌 서버에서 추출
    Long userId = SecurityUtils.getCurrentUserIdOrThrow();
    learningService.heartbeat(userId, ...);
}
```

---

## 4. 변경된 핵심 파일 목록

### 신규 생성
- `src/main/java/org/zerock/projecttraveler/entity/*.java` (8개)
- `src/main/java/org/zerock/projecttraveler/repository/*.java` (8개)
- `src/main/java/org/zerock/projecttraveler/service/*.java` (7개)
- `src/main/java/org/zerock/projecttraveler/controller/**/*.java` (8개)
- `src/main/java/org/zerock/projecttraveler/dto/*.java` (7개)
- `src/main/java/org/zerock/projecttraveler/security/*.java` (3개)
- `src/main/java/org/zerock/projecttraveler/config/*.java` (3개)
- `src/main/resources/templates/register.html`
- `src/main/resources/templates/admin/*.html` (3개)

### 수정
- `build.gradle` - 의존성 추가
- `application.properties` - DB/Security 설정
- `src/main/resources/templates/index.html` - Spring Security 연동
- `src/main/resources/templates/main.html` - 동적 데이터 바인딩
- `src/main/resources/templates/course-detail.html` - 동적 커리큘럼
- `src/main/resources/templates/fragments/main-header.html` - 관리자 메뉴

### 삭제
- `src/main/java/.../controller/PageController.java` (MainPageController로 대체)

---

## 5. 향후 확장 포인트

### 5.1 마이페이지 확장
- `DashboardService`의 메서드들은 마이페이지에서 재사용 가능
- 프로필 수정, 비밀번호 변경 API 추가 필요

### 5.2 그룹 배정
- `UserGroup` 엔티티 추가
- `EnrollmentAdminService.assignToGroup(groupId, courseId)` 메서드 추가

### 5.3 HLS/S3 전환
- `Lesson.videoType`에 `HLS` 추가
- S3 업로드 서비스 구현
- 프론트엔드 HLS.js 연동

### 5.4 영상 재생 페이지
- `/lesson` 라우트 추가
- `<video>` 태그 + JavaScript로 heartbeat 10초마다 호출
- `timeupdate` 이벤트로 재생 위치 추적
