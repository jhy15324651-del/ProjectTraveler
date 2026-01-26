# LMS API 명세서 (API Specification)

## 공통 사항

### Base URL
```
http://localhost:8080
```

### 인증
- 모든 API는 로그인이 필요합니다 (세션 기반)
- 관리자 API(`/api/admin/**`, `/admin/**`)는 ROLE_ADMIN 권한 필요

### 응답 형식
```json
{
  "success": true/false,
  "message": "응답 메시지",
  "data": { ... }  // 선택적
}
```

---

## 1. 인증 API

### 1.1 회원가입
```
POST /api/auth/register
```

**Request Body:**
```json
{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com",
  "fullName": "홍길동"
}
```

**Response (성공):**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다."
}
```

**Response (실패):**
```json
{
  "success": false,
  "message": "이미 사용 중인 아이디입니다."
}
```

---

## 2. 수강 API

### 2.1 수강 신청
```
POST /api/enrollments/request
```

**Request Body:**
```json
{
  "courseId": 1
}
```

**Response (SELF 정책):**
```json
{
  "success": true,
  "message": "수강 신청이 완료되었습니다. 바로 학습을 시작할 수 있습니다.",
  "data": {
    "id": 1,
    "userId": 2,
    "courseId": 1,
    "status": "APPROVED",
    "statusDisplayName": "수강 중",
    "source": "USER_REQUEST"
  }
}
```

**Response (APPROVAL 정책):**
```json
{
  "success": true,
  "message": "수강 신청이 접수되었습니다. 관리자 승인 후 학습이 가능합니다.",
  "data": {
    "status": "REQUESTED",
    "statusDisplayName": "승인 대기"
  }
}
```

**Error Cases:**
- `400`: 이미 수강 신청한 강좌입니다.
- `400`: 이 강좌는 관리자 배정만 가능합니다. (ASSIGN_ONLY)

### 2.2 수강 상태 조회
```
GET /api/enrollments/status?courseId=1
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "courseId": 1,
    "courseTitle": "일본어 기초 회화",
    "status": "APPROVED",
    "statusDisplayName": "수강 중",
    "progressPercent": 45,
    "completedLessonCount": 5,
    "totalLessonCount": 12,
    "totalWatchedSec": 2700,
    "totalWatchedFormatted": "45분"
  }
}
```

---

## 3. 학습 API

### 3.1 Heartbeat (영상 학습 추적)
```
POST /api/learning/heartbeat
```

**Request Body:**
```json
{
  "courseId": 1,
  "lessonId": 5,
  "positionSec": 120,
  "deltaWatchedSec": 15
}
```

**Response:**
```json
{
  "success": true,
  "message": "학습 기록이 저장되었습니다."
}
```

**호출 주기:** 10~15초마다

### 3.2 레슨 완료 처리
```
POST /api/learning/complete
```

**Request Body:**
```json
{
  "courseId": 1,
  "lessonId": 5
}
```

**Response (성공):**
```json
{
  "success": true,
  "message": "레슨을 완료했습니다.",
  "data": {
    "completed": true
  }
}
```

**Response (조건 미충족):**
```json
{
  "success": true,
  "message": "아직 완료 조건을 충족하지 못했습니다.",
  "data": {
    "completed": false
  }
}
```

**완료 조건:** 영상 길이의 90% 이상 시청

### 3.3 레슨 진도 조회
```
GET /api/learning/progress?courseId=1&lessonId=5
```

**Response:**
```json
{
  "success": true,
  "data": {
    "lastPositionSec": 120,
    "watchedSec": 300,
    "completed": false,
    "progressPercent": 33
  }
}
```

---

## 4. 출석 API

### 4.1 출석 체크
```
POST /api/attendance/check-in
```

**Response (신규 출석):**
```json
{
  "success": true,
  "message": "출석 체크 완료!",
  "data": {
    "totalDays": 25,
    "consecutiveDays": 6,
    "thisMonthDays": 19,
    "attendanceRate": 76,
    "checkedInToday": true
  }
}
```

**Response (이미 출석):**
```json
{
  "success": true,
  "message": "이미 오늘 출석했습니다.",
  "data": { ... }
}
```

### 4.2 출석 터치 (학습 시 자동 호출)
```
POST /api/attendance/touch
```

**Response:**
```json
{
  "success": true,
  "message": "출석이 기록되었습니다."
}
```

### 4.3 출석 통계 조회
```
GET /api/attendance/stats
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalDays": 25,
    "consecutiveDays": 6,
    "thisMonthDays": 19,
    "attendanceRate": 76,
    "checkedInToday": true
  }
}
```

### 4.4 월별 출석 현황
```
GET /api/attendance/monthly?year=2026&month=1
```

**Response:**
```json
{
  "success": true,
  "data": {
    "year": 2026,
    "month": 1,
    "days": [
      { "day": 1, "attended": true, "today": false, "future": false },
      { "day": 2, "attended": false, "today": false, "future": false },
      ...
      { "day": 26, "attended": true, "today": true, "future": false },
      { "day": 27, "attended": false, "today": false, "future": true }
    ]
  }
}
```

---

## 5. 대시보드 API

### 5.1 나의 학습 요약
```
GET /api/dashboard/my-learning
```

**Response:**
```json
{
  "success": true,
  "data": {
    "inProgressCourseCount": 3,
    "completedCourseCount": 2,
    "averageProgressPercent": 58,
    "consecutiveAttendanceDays": 6,
    "totalLearningTimeSec": 16200,
    "totalLearningTimeFormatted": "4시간 30분",
    "totalAttendanceDays": 25,
    "thisMonthAttendanceDays": 19,
    "attendanceRate": 76
  }
}
```

---

## 6. 관리자 API

### 6.1 수강 승인
```
POST /admin/enrollments/api/{id}/approve
```

**Request Body (선택):**
```json
{
  "note": "승인합니다."
}
```

**Response:**
```json
{
  "success": true,
  "message": "수강이 승인되었습니다.",
  "data": {
    "id": 1,
    "status": "APPROVED"
  }
}
```

### 6.2 수강 반려
```
POST /admin/enrollments/api/{id}/reject
```

**Request Body (선택):**
```json
{
  "note": "정원 초과로 반려합니다."
}
```

**Response:**
```json
{
  "success": true,
  "message": "수강이 반려되었습니다.",
  "data": {
    "id": 1,
    "status": "REJECTED"
  }
}
```

### 6.3 강좌 배정
```
POST /admin/enrollments/api/assign
```

**Request Body:**
```json
{
  "courseId": 1,
  "userIds": [2, 3, 4],
  "note": "필수 교육 배정"
}
```

**Response:**
```json
{
  "success": true,
  "message": "3명에게 강좌가 배정되었습니다.",
  "data": 3
}
```

### 6.4 강좌 생성
```
POST /admin/courses/api
```

**Request Body:**
```json
{
  "title": "새 강좌",
  "shortDesc": "강좌 설명",
  "fullDesc": "상세 설명",
  "thumbnailUrl": "https://...",
  "category": "LANGUAGE",
  "level": "BEGINNER",
  "enrollPolicy": "SELF"
}
```

### 6.5 레슨 영상 업로드
```
POST /admin/courses/api/lessons/{lessonId}/video
Content-Type: multipart/form-data
```

**Form Data:**
- `file`: 영상 파일 (MP4)
- `durationSec`: 영상 길이 (초) - 선택

**Response:**
```json
{
  "success": true,
  "message": "영상이 업로드되었습니다.",
  "data": {
    "id": 1,
    "videoType": "MP4",
    "videoUrl": "/uploads/videos/abc123.mp4"
  }
}
```

---

## 7. 에러 코드

| HTTP 상태 | 설명 |
|-----------|------|
| 200 | 성공 |
| 400 | 잘못된 요청 (유효성 검사 실패, 비즈니스 로직 오류) |
| 401 | 인증 필요 (로그인 필요) |
| 403 | 권한 없음 (ADMIN 권한 필요) |
| 404 | 리소스를 찾을 수 없음 |
| 500 | 서버 오류 |
