# 팀 DB 협업 가이드 (Team Database Guide)

LMS 프로젝트 팀원을 위한 데이터베이스 협업 가이드입니다.

---

## 1. 기본 원칙

### 1.1 각자 로컬 DB 사용
- 모든 팀원은 자신의 PC에 MariaDB를 설치하고 **로컬 DB**를 사용합니다.
- DB 이름: `lms_local` (통일)
- 다른 팀원의 DB에 직접 접속하지 않습니다.

### 1.2 Git과 DB 분리
- **코드**: Git으로 공유
- **DB 데이터**: 각자 로컬에서 관리
- **DB 스키마**: JPA Entity가 관리 (ddl-auto=update)

### 1.3 환경 분리
```
개발: application-local.yml → 로컬 MariaDB
운영: application-prod.yml → 클라우드 DB (추후)
```

---

## 2. 로컬 환경 설정 절차

### 2.1 MariaDB 설치
1. https://mariadb.org/download/ 에서 다운로드
2. 설치 시 root 비밀번호를 `1234`로 설정 (팀 통일)
3. 포트는 기본값 `3306` 유지

### 2.2 데이터베이스 생성
```sql
CREATE DATABASE lms_local
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

### 2.3 초기 데이터 생성
**방법 1: JPA 자동 생성 + DataInitializer**
- 앱 실행만 하면 테이블과 초기 데이터가 자동 생성됨

**방법 2: SQL 스크립트 실행**
```bash
mysql -u root -p < docs/db/schema.sql
mysql -u root -p lms_local < docs/db/seed.sql
```

### 2.4 프로필 확인
`application.yml`의 `spring.profiles.active`가 `local`인지 확인:
```yaml
spring:
  profiles:
    active: local
```

---

## 3. 스키마 통일 규칙

### 3.1 DB/테이블 명명 규칙
| 항목 | 규칙 | 예시 |
|------|------|------|
| 데이터베이스 | snake_case | `lms_local`, `lms_prod` |
| 테이블 | snake_case | `course_enrollment` |
| 컬럼 | snake_case | `created_at`, `user_id` |
| 인덱스 | idx_{테이블}_{컬럼} | `idx_enrollment_user` |

### 3.2 데이터 타입 통일
| 용도 | 타입 |
|------|------|
| 기본 키 | `BIGINT AUTO_INCREMENT` |
| 문자열 (짧은) | `VARCHAR(n)` |
| 문자열 (긴) | `TEXT` |
| 날짜+시간 | `DATETIME` |
| 날짜만 | `DATE` |
| Boolean | `BOOLEAN` (TINYINT(1)) |
| 정수 | `INT` |

### 3.3 필수 컬럼
모든 테이블에 포함 권장:
- `id` - 기본 키
- `created_at` - 생성 일시
- `updated_at` - 수정 일시 (선택)

---

## 4. Git 관리 규칙

### 4.1 커밋하면 안 되는 파일
`.gitignore`에 포함되어야 할 파일:

```gitignore
# 개인 설정
application-local.yml      # 로컬 DB 비밀번호 포함
application-prod.yml       # 운영 DB 비밀번호 포함

# 업로드 파일
/uploads/
*.mp4
*.avi
*.mov

# IDE 설정
.idea/
*.iml
.vscode/

# 빌드 결과물
/build/
/target/

# 로그 파일
*.log
logs/
```

### 4.2 커밋해야 하는 파일
```
application.yml            # 공통 설정 (비밀번호 없음)
docs/db/schema.sql         # 스키마 정의
docs/db/seed.sql           # 초기 데이터
```

### 4.3 설정 파일 관리 방식
```
application.yml           ← Git에 커밋 (공통 설정)
application-local.yml     ← Git에 커밋하지 않음 (각자 생성)
application-prod.yml      ← Git에 커밋하지 않음 (운영 담당자만)
```

**팀원이 처음 프로젝트를 클론받으면:**
1. `application-local.yml.example`을 복사
2. `application-local.yml`로 이름 변경
3. 자신의 DB 정보로 수정

---

## 5. 스키마 변경 시 프로세스

### 5.1 Entity 변경 시
1. Entity 클래스 수정
2. 로컬에서 테스트 (ddl-auto=update가 자동 반영)
3. PR 생성 시 스키마 변경 내용 명시
4. 팀원들은 PR 머지 후 앱 재시작

### 5.2 주의사항
- **컬럼 삭제**: ddl-auto=update는 삭제를 반영하지 않음
  - 직접 `ALTER TABLE ... DROP COLUMN` 실행 필요
- **컬럼 이름 변경**: 새 컬럼 생성 + 기존 컬럼 유지됨
  - 데이터 마이그레이션 스크립트 필요

### 5.3 대규모 스키마 변경 시
1. `docs/db/migration/` 폴더에 마이그레이션 SQL 작성
2. 파일명: `V{번호}__{설명}.sql` (예: `V002__add_user_profile.sql`)
3. 팀 채널에 공지

---

## 6. 업로드 파일 처리

### 6.1 로컬 저장 경로 통일
```yaml
# application-local.yml
app:
  upload:
    video-path: C:/lms-uploads/videos   # Windows
    # video-path: ~/lms-uploads/videos  # Mac/Linux
```

### 6.2 업로드 폴더 구조
```
C:/lms-uploads/
├── videos/          # 강의 영상
├── thumbnails/      # 강좌 썸네일
└── temp/            # 임시 파일
```

### 6.3 업로드 파일은 Git에 포함하지 않음
- 대용량이므로 Git 관리 비효율적
- 팀 간 파일 공유 필요 시 Google Drive 등 사용

---

## 7. 트러블슈팅

### 7.1 "Access denied" 에러
```
원인: DB 비밀번호 불일치
해결: application-local.yml의 password 확인
```

### 7.2 "Unknown database 'lms_local'" 에러
```
원인: DB가 생성되지 않음
해결: CREATE DATABASE lms_local ... 실행
```

### 7.3 테이블 생성 안 됨
```
원인: ddl-auto 설정 또는 Entity 스캔 문제
해결:
1. ddl-auto=update 확인
2. @Entity 어노테이션 확인
3. 패키지 경로 확인
```

### 7.4 "Duplicate entry" 에러
```
원인: UNIQUE 제약조건 위반
해결: 데이터 정리 또는 seed.sql의 ON DUPLICATE KEY 사용
```

### 7.5 한글 깨짐
```
원인: 인코딩 불일치
해결:
1. DB: utf8mb4
2. JDBC URL: characterEncoding=UTF-8
3. 테이블: CHARSET=utf8mb4
```

---

## 8. 새 팀원 온보딩 체크리스트

새 팀원이 개발 환경을 설정할 때:

- [ ] Git 저장소 클론
- [ ] MariaDB 설치 (root / 1234)
- [ ] 데이터베이스 생성 (`lms_local`)
- [ ] `application-local.yml` 생성 (example 파일 복사)
- [ ] 업로드 폴더 생성 (`C:/lms-uploads/videos`)
- [ ] 앱 실행 후 테이블 자동 생성 확인
- [ ] 로그인 테스트 (admin / admin123)

---

## 9. Unity 연동 참고

### Unity는 DB에 직접 접속하지 않습니다!

```
[Unity] ──HTTP──> [Spring Boot API] ──JDBC──> [MariaDB]
```

- Unity는 REST API만 호출
- DB 위치가 바뀌어도 Unity 코드 변경 불필요
- API 서버 URL만 변경하면 됨

### Unity에서 필요한 정보
```
API Base URL: http://localhost:8080/api
또는
API Base URL: https://your-server.com/api
```

---

## 연락처

DB 관련 문의: 팀 리드 또는 Slack #db-help 채널
