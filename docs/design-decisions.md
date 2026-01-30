# LMS 리팩터링 설계 결정 문서

## 개요

이 문서는 온라인학습 페이지 리팩터링 및 마이페이지 통합 작업에서 내린 설계 결정들의 근거를 설명합니다.

---

## 1. 패키지 구조 선택 이유

### 결정
```
controller/
  ├── api/           # REST API 컨트롤러
  │   ├── QuizApiController
  │   ├── CertificateApiController
  │   └── ...
  └── MainPageController  # 페이지 라우팅
service/
  ├── QuizService
  ├── CertificateService
  └── ...
repository/
entity/
dto/
```

### 근거
1. **기존 구조 유지**: 프로젝트에 이미 확립된 패키지 구조(controller/service/repository/entity/dto)를 유지하여 일관성 확보
2. **API/Page 분리**: REST API는 `controller/api/` 하위에 배치하여 페이지 컨트롤러와 구분
3. **충돌 최소화**: 기존 코드를 수정하기보다 새 기능을 추가하는 방식으로 접근

---

## 2. 영상 URL 저장 및 변환 처리 이유

### 결정
- Lesson 엔티티에 원본 YouTube URL을 그대로 저장
- 서버 측(MainPageController)에서 embed URL로 변환하여 템플릿에 전달

### 근거
1. **유연성**: 원본 URL을 저장하면 나중에 다른 형식(watch, share, shorts 등)으로 입력해도 대응 가능
2. **데이터 정규화 불필요**: 사용자/관리자가 다양한 형태로 URL을 입력해도 시스템이 알아서 처리
3. **클라이언트 부담 감소**: JavaScript에서 URL 파싱하지 않고 서버에서 처리
4. **보안**: YouTube API 키 없이 embed 방식으로 안전하게 재생

### 변환 로직 (MainPageController:314-349)
```java
// 지원하는 URL 형태:
// - youtube.com/watch?v=VIDEO_ID
// - youtu.be/VIDEO_ID
// - youtube.com/embed/VIDEO_ID (이미 embed면 그대로 반환)
```

---

## 3. 진도율을 레슨 완료 기반으로 계산한 이유

### 결정
```
진도율 = (완료한 레슨 수 / 전체 레슨 수) × 100
```

### 근거
1. **명확한 기준**: "90% 이상 시청 = 레슨 완료"라는 단일 기준으로 일관성 유지
2. **하드코딩 제거**: 템플릿에서 `45%`, `80%` 등 고정값 대신 실시간 계산값 사용
3. **확장성**: 레슨 단위 완료 여부만 추적하면 강좌 진도율은 자동 계산
4. **성능**: 복잡한 시청 시간 합산 대신 boolean 필드(completed) 카운트로 빠른 조회

### 완료 기준 (LearningService:COMPLETION_THRESHOLD)
```java
private static final double COMPLETION_THRESHOLD = 0.9;  // 90%
```

---

## 4. SELF / APPROVAL 정책을 enum으로 분리한 이유

### 결정
```java
public enum EnrollPolicy {
    SELF,           // 즉시 수강 가능
    APPROVAL,       // 관리자 승인 필요
    ASSIGN_ONLY     // 관리자 배정만 가능
}
```

### 근거
1. **명시적 정책 표현**: 문자열 대신 enum으로 타입 안전성 확보
2. **비즈니스 로직 캡슐화**: 정책에 따른 분기를 enum 기반으로 처리
3. **확장 용이**: 새 정책(예: `PAID`, `COUPON_ONLY`) 추가 시 enum에만 추가
4. **수료증 연동**: APPROVAL 정책 강좌만 수료증 발급 가능하도록 조건 분리

### UI 분기 예시
- SELF: "바로 학습하기" 버튼 → 즉시 Enrollment 생성 (ENROLLED)
- APPROVAL: "수강신청" 버튼 → Enrollment 생성 (REQUESTED) → 관리자 승인 후 학습 가능

---

## 5. 출석을 학습 이벤트 기반으로 한 이유

### 결정
- 별도의 "출석 버튼" 클릭 없이, 학습 활동 발생 시 자동 출석 처리
- 트리거: heartbeat 전송, 레슨 완료, 퀴즈 제출

### 근거
1. **실제 학습 추적**: 단순 페이지 방문이 아닌 실제 영상 시청/학습 활동 기반
2. **사용자 편의**: 출석 버튼을 찾아 누르는 번거로움 제거
3. **정확성**: 영상 재생 중 주기적(10초) heartbeat로 실제 학습 시간 추적
4. **유연성**: AttendanceService.touchAttendance()를 여러 서비스에서 호출 가능

### 구현 위치
- `LearningService.heartbeat()`: 영상 재생 중 자동 출석
- `QuizService.submitQuiz()`: 퀴즈 제출 시 출석

---

## 6. 수료증을 승인형 강의에만 적용한 이유

### 결정
- `Course.enrollPolicy == APPROVAL`인 강좌만 수료증 발급 가능
- 조건: 진도율 90% 이상 + 퀴즈 90% 이상

### 근거
1. **공식성**: 승인형 강좌는 관리자가 심사/배정하는 공식 교육과정
2. **가치 부여**: 누구나 바로 수강 가능한 SELF 강좌와 차별화
3. **관리 효율**: 수료증 남발 방지, 의미 있는 완료 인증
4. **비즈니스 로직**: 유료/기업 교육에서 수료증 발급이 필요한 경우 APPROVAL 정책 사용

### 발급 조건 (CertificateService)
```java
private static final int REQUIRED_PROGRESS_PERCENT = 90;
private static final int REQUIRED_QUIZ_PERCENT = 90;
```

---

## 7. `/learning` vs `/online-learning` 역할 분리

### 결정
| 경로 | 역할 | 대상 |
|------|------|------|
| `/learning` | 탐색 허브 | 전체 강좌 목록 (카테고리별) |
| `/online-learning` | 내 수강 현황 | 수강 중/완료 강좌 + 진도율 |

### 근거
1. **사용자 의도 분리**:
   - "새로운 강좌를 찾고 싶다" → `/learning`
   - "내가 듣고 있는 강좌를 이어서 학습하고 싶다" → `/online-learning`
2. **정보 과부하 방지**: 모든 강좌 + 내 진도를 한 페이지에 넣으면 복잡
3. **네비게이션 일관성**: 서브 네비의 "온라인학습"은 개인화된 학습 대시보드

---

## 8. 향후 확장 로드맵

### Phase 2: 영상 업로드 지원
- Lesson.videoType에 UPLOAD 추가
- S3 또는 로컬 스토리지 연동
- 트랜스코딩 파이프라인 (mp4 → HLS)

### Phase 3: HLS 스트리밍
- CDN 연동
- 적응형 비트레이트 스트리밍
- 시청 구간 추적 고도화

### Phase 4: DRM 보호
- Widevine/FairPlay 연동
- 다운로드 방지
- 화면 녹화 차단

### Phase 5: 학습 분석
- 사용자별 학습 패턴 분석
- 강좌 완료율 통계
- 드롭아웃 포인트 분석

---

## 파일/패키지 변경 요약

### 새로 생성된 파일
```
entity/
  ├── Quiz.java
  ├── QuizQuestion.java
  ├── QuizOption.java
  ├── QuizAttempt.java
  ├── QuizAnswer.java
  └── Certificate.java

repository/
  ├── QuizRepository.java
  ├── QuizQuestionRepository.java
  ├── QuizOptionRepository.java
  ├── QuizAttemptRepository.java
  ├── QuizAnswerRepository.java
  └── CertificateRepository.java

service/
  ├── QuizService.java
  └── CertificateService.java

controller/api/
  ├── QuizApiController.java
  └── CertificateApiController.java

dto/
  ├── QuizDto.java
  └── CertificateDto.java
```

### 수정된 파일
```
controller/MainPageController.java
  - onlineLearning(): 내 수강 목록 기반으로 변경
  - myClassroom(): 수료증 목록 추가

templates/online-learning.html
  - 하드코딩 제거, Thymeleaf로 DB 기반 렌더링

templates/my-classroom.html
  - 수료증 섹션 추가
```

### 제거된 하드코딩 코드
```
templates/online-learning.html:30-106
  - 6개의 정적 강좌 카드 (일본어 기초, 일본어 중급, 일본 문화, 도쿄 여행 등)
  - 하드코딩된 진도율 (45%, 20%, 80%, 65%, 90%, 35%)
```

---

## API 엔드포인트 요약

### 퀴즈 API
- `GET /api/quiz/course/{courseId}` - 강좌의 퀴즈 조회
- `GET /api/quiz/{quizId}` - 퀴즈 상세 조회
- `POST /api/quiz/submit` - 퀴즈 제출
- `GET /api/quiz/status/{courseId}` - 퀴즈 상태 조회
- `GET /api/quiz/{quizId}/history` - 시도 기록 조회

### 수료증 API
- `GET /api/certificates` - 내 수료증 목록
- `GET /api/certificates/{id}` - 수료증 상세
- `GET /api/certificates/verify/{number}` - 수료증 검증
- `GET /api/certificates/eligibility/{courseId}` - 발급 자격 확인
- `POST /api/certificates/issue` - 수료증 발급

---

*최종 수정: 2026-01-29*
