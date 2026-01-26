# LMS 구현 요약 (Implementation Summary)

## 1. 프로젝트 개요

**일본 여행 LMS (Learning Management System)**

일본 여행을 준비하는 학습자를 위한 온라인 학습 관리 시스템입니다.
기초 일본어 회화, 일본 문화, 여행 정보 등을 강좌 형태로 제공하며,
실무 LMS에서 흔히 사용되는 수강 신청/승인/배정, 진도 관리, 출석 체크 기능을 구현했습니다.

---

## 2. 합의된 요구사항

### 2.1 혼합형 수강 모델 (Approval + Assignment)

| 방식 | 설명 | 사용 사례 |
|------|------|-----------|
| **SELF** | 사용자가 수강 신청하면 즉시 승인 | 일반 공개 강좌 |
| **APPROVAL** | 관리자 승인 후 수강 가능 | 선택 교육, 정원 제한 강좌 |
| **ASSIGN_ONLY** | 관리자 배정만 가능 | 필수 교육, 패키지 강의 |

### 2.2 권한 분리 (RBAC)

| 역할 | 권한 |
|------|------|
| **ADMIN** | 강좌/레슨/영상 CRUD, 수강 승인/반려/배정, 전체 관리 |
| **USER** | 수강 신청, 학습, 진도 기록, 출석 체크 |

### 2.3 핵심 기능

1. **수강 관리**: 신청 → 승인 → 학습 → 완료
2. **동영상 학습**: 영상 재생, 진도 추적, 이어서 학습
3. **출석 관리**: 일일 출석 체크, 연속 출석일, 통계
4. **대시보드**: 수강 현황, 평균 진도율, 학습 시간 집계

---

## 3. 최종 구현 범위

### 3.1 Backend

| 구분 | 구현 내용 |
|------|-----------|
| **Entity** | User, Course, CourseUnit, Lesson, CourseEnrollment, LessonProgress, LearningSession, AttendanceDaily |
| **Repository** | 각 Entity에 대한 JPA Repository (커스텀 쿼리 포함) |
| **Service** | UserService, CourseService, EnrollmentService, EnrollmentAdminService, LearningService, AttendanceService, DashboardService |
| **Controller** | AuthController, MainPageController, EnrollmentApiController, LearningApiController, AttendanceApiController, DashboardApiController, AdminEnrollmentController, AdminCourseController |
| **Security** | Spring Security 기반 인증/인가, BCrypt 암호화, Remember-Me |

### 3.2 Frontend

| 페이지 | 설명 |
|--------|------|
| `/login` | 로그인 페이지 (Spring Security 연동) |
| `/register` | 회원가입 페이지 |
| `/main` | 메인 대시보드 (학습 현황 통계) |
| `/learning` | 강좌 목록 |
| `/course-detail` | 강좌 상세 (커리큘럼, 진도, 수강 신청) |
| `/my-classroom` | 나의 강의실 (수강 중/완료 강좌) |
| `/attendance` | 출석 체크 (캘린더, 통계) |
| `/admin/*` | 관리자 페이지 (강좌 관리, 수강 승인, 배정) |

### 3.3 Database

- **DBMS**: MariaDB
- **ORM**: Spring Data JPA + Hibernate
- **DDL**: `spring.jpa.hibernate.ddl-auto=update` (자동 스키마 생성)

---

## 4. 화면별 동작 흐름

### 4.1 학습자 플로우

```
로그인 → 메인(대시보드) → 강좌 목록 → 강좌 상세
                                          ↓
                              [수강 신청 버튼 클릭]
                                          ↓
                          (SELF) 즉시 수강 가능
                          (APPROVAL) 관리자 승인 대기
                                          ↓
                              [이어서 학습하기 클릭]
                                          ↓
                              영상 재생 + 진도 추적
                                          ↓
                              레슨 완료 → 다음 레슨
                                          ↓
                              전체 완료 → 수강 완료 처리
```

### 4.2 관리자 플로우

```
관리자 로그인 → 관리자 메뉴 접근
        ↓
  ┌─────┴─────┐
  ↓           ↓
강좌 관리   수강 관리
  ↓           ↓
CRUD      승인/반려/배정
```

---

## 5. 보안 적용 사항

| 항목 | 구현 |
|------|------|
| **인증** | Spring Security FormLogin |
| **비밀번호** | BCrypt 암호화 |
| **권한 체크** | URL 패턴 + @PreAuthorize 어노테이션 |
| **CSRF** | API 경로만 비활성화, 폼은 CSRF 토큰 사용 |
| **userId 조작 방지** | SecurityContext에서 서버가 강제 추출 |
| **세션 관리** | 단일 세션 정책, 세션 만료 처리 |

---

## 6. 데모 계정

| 역할 | 아이디 | 비밀번호 |
|------|--------|----------|
| 관리자 | admin | admin123 |
| 사용자 | user1 | user123 |
| 사용자 | user2 | user123 |

---

## 7. 주요 기술 스택

- **Backend**: Spring Boot 4.0.1, Java 17
- **Security**: Spring Security 6
- **ORM**: Spring Data JPA
- **Database**: MariaDB
- **Template**: Thymeleaf
- **Build**: Gradle

---

## 8. 향후 확장 포인트

1. **영상 재생 페이지**: `<video>` 태그 기반 MP4 재생 + heartbeat API 연동
2. **HLS/S3 전환**: 대용량 영상 스트리밍 최적화
3. **그룹 배정**: 부서/조직 단위 일괄 배정
4. **마이페이지**: 프로필 수정, 학습 이력 상세
5. **수료증 발급**: PDF 생성 기능
