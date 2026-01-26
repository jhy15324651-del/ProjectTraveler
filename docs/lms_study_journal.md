# LMS 개발 학습 일지 (Study Journal)

## Day 1: 프로젝트 구조 분석 및 설계

### 오늘 구현한 것
- 기존 프로젝트 코드 분석 (Frontend UI만 완성된 상태 확인)
- DB 스키마 설계 (8개 테이블)
- 혼합형 수강 모델 설계 (SELF/APPROVAL/ASSIGN_ONLY)

### 배운 것
- **실무 LMS 구조**: 단순 수강 신청보다 승인/배정 기능이 필요한 이유
  - 선택 교육 vs 필수 교육 운영
  - 정원 관리, 수강 자격 확인
- **Enrollment 상태머신**: REQUESTED → APPROVED → COMPLETED 흐름
- **권한 분리의 중요성**: ADMIN/USER 역할에 따른 기능 제한

### 막혔던 것
- Spring Boot 4.x에서 `spring-boot-starter-webmvc` vs `spring-boot-starter-web` 차이
  - `webmvc`는 비표준, `web`이 올바른 의존성

### 다음 할 것
- Entity 클래스 구현
- Repository 인터페이스 작성

---

## Day 2: Entity/Repository/Security 구현

### 오늘 구현한 것
- 8개 Entity 클래스 (User, Course, CourseUnit, Lesson, CourseEnrollment, LessonProgress, LearningSession, AttendanceDaily)
- 8개 Repository 인터페이스 (커스텀 쿼리 포함)
- Spring Security 설정

### 배운 것
- **JPA 연관관계 매핑**: `@ManyToOne`, `@OneToMany`
- **JPQL 커스텀 쿼리**: `@Query` 어노테이션 사용법
- **Spring Security 6 설정**: 람다 DSL 스타일 (`HttpSecurity` 설정)

### 핵심 키워드 체크
- [x] Entity 설계 (1:N, N:1 관계)
- [x] JPA Repository (커스텀 쿼리)
- [x] Spring Security (FormLogin)
- [x] BCrypt 암호화

### 막혔던 것
- `spring-boot-starter-security`에서 자동으로 모든 경로가 인증 필요로 설정됨
  - `SecurityFilterChain` Bean으로 명시적 설정 필요
- `@PreAuthorize`가 동작하려면 `@EnableMethodSecurity` 필요

### 다음 할 것
- Service 계층 구현
- Controller 구현

---

## Day 3: Service/Controller 구현

### 오늘 구현한 것
- 7개 Service 클래스
- 8개 Controller 클래스 (페이지 + REST API)
- 관리자 기능 (승인/반려/배정)

### 배운 것
- **서비스 계층 분리**: 재사용성을 위한 DTO 변환
  - `EnrollmentService.toEnrollmentDtoWithProgress()` - 진도 정보 포함
  - `DashboardService.getMyLearningSummary()` - 마이페이지에서 재사용 가능
- **userId 보안**: 클라이언트가 조작하지 못하도록 서버에서 강제 추출
  ```java
  Long userId = SecurityUtils.getCurrentUserIdOrThrow();
  ```
- **@Transactional 전략**: 읽기 전용은 `readOnly=true`, 쓰기는 기본값

### 핵심 키워드 체크
- [x] RBAC (ROLE_ADMIN/ROLE_USER)
- [x] Enrollment 상태머신 (REQUESTED → APPROVED)
- [x] 서비스 계층 재사용성
- [x] DTO 패턴

### 막혔던 것
- 순환 참조 문제: `LearningService` ↔ `AttendanceService`
  - `touchAttendance()`를 별도 호출로 분리하여 해결
- Thymeleaf에서 `#lists.isEmpty()` 사용 시 import 불필요

### 다음 할 것
- 템플릿 동적 데이터 바인딩
- 문서 작성

---

## Day 4: 템플릿 연동 및 마무리

### 오늘 구현한 것
- 기존 HTML 템플릿에 동적 데이터 바인딩
- 관리자 페이지 템플릿 3개 생성
- 회원가입 페이지 추가
- 초기 데이터 생성기 (DataInitializer)

### 배운 것
- **Thymeleaf 조건부 렌더링**:
  ```html
  <th:block th:if="${course.enrollmentInfo != null and course.enrollmentInfo.accessible}">
  ```
- **Thymeleaf 인라인 JavaScript**:
  ```javascript
  const courseId = /*[[${course.id}]]*/ 0;
  ```
- **Spring Security + Thymeleaf**: 로그아웃은 POST 방식 필수

### 핵심 키워드 체크
- [x] 데이터 무결성 (UNIQUE 제약조건)
- [x] 서버에서 userId 강제 추출
- [x] 대시보드 집계 쿼리
- [x] Thymeleaf 동적 바인딩

### 막혔던 것
- Thymeleaf에서 `th:onclick`에 변수 전달:
  ```html
  th:onclick="|selectLesson(${course.id}, ${lesson.id})|"
  ```
- CSS 변수 `var(--primary-red)` 사용 시 `:root`에 정의 필요

---

## 학습 요약

### 핵심 개념 정리

1. **RBAC (Role-Based Access Control)**
   - 역할 기반 접근 제어
   - Spring Security의 `hasRole('ADMIN')` 사용

2. **Enrollment 상태머신**
   - 상태: REQUESTED → APPROVED/REJECTED → COMPLETED
   - 배정: ASSIGNED (관리자가 직접 할당)

3. **영상 학습 추적 (Heartbeat)**
   - 10~15초마다 서버에 진도 전송
   - `lastPositionSec`: 마지막 재생 위치 (이어보기용)
   - `watchedSec`: 누적 시청 시간 (완료 판정용)

4. **데이터 무결성**
   - `UNIQUE(user_id, course_id)`: 중복 수강 방지
   - `UNIQUE(user_id, lesson_id)`: 중복 진도 방지
   - `UNIQUE(user_id, attend_date)`: 중복 출석 방지

5. **대시보드 집계**
   - `COUNT()`, `SUM()`, `AVG()` 활용
   - 연속 출석일은 날짜 리스트를 받아서 Java에서 계산

### 앞으로 공부할 것

1. **영상 재생 페이지 구현**
   - HTML5 `<video>` 태그
   - JavaScript `timeupdate` 이벤트
   - heartbeat API 주기적 호출

2. **HLS 스트리밍**
   - FFmpeg로 MP4 → HLS 변환
   - AWS S3 + CloudFront 연동
   - hls.js 라이브러리

3. **성능 최적화**
   - 대시보드 캐싱 (Redis)
   - 진도 데이터 배치 저장

4. **테스트 코드**
   - JUnit 5 + Mockito
   - `@WebMvcTest`, `@DataJpaTest`
