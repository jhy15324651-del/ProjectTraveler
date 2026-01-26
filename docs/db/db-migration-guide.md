# DB 이전 가이드 (Database Migration Guide)

이 문서는 로컬 MariaDB에서 다른 환경(다른 PC, 클라우드 DB)으로 데이터를 이전하는 방법을 설명합니다.

---

## 1. 이전 시나리오

### 시나리오 A: 로컬 PC → 다른 로컬 PC
```
[학원 PC의 MariaDB] → dump → [내 노트북의 MariaDB]
```

### 시나리오 B: 로컬 PC → 클라우드 DB
```
[학원 PC의 MariaDB] → dump → [Railway/AWS RDS]
```

### 시나리오 C: 클라우드 DB 사용 (팀 공용)
```
[모든 팀원] → 인터넷 → [클라우드 DB]
```

---

## 2. 사전 체크리스트

이전 작업 전에 반드시 확인하세요:

- [ ] 원본 DB 백업 완료
- [ ] 대상 DB 서버 접속 확인
- [ ] DB 이름, 계정, 비밀번호 확인
- [ ] 네트워크 연결 확인 (클라우드의 경우 방화벽/보안그룹)
- [ ] 충분한 디스크 공간 확인

---

## 3. mysqldump를 이용한 백업/복원

### 3.1 데이터 백업 (Export)

```bash
# 전체 백업 (스키마 + 데이터)
mysqldump -u root -p lms_local > backup_full.sql

# 스키마만 백업 (테이블 구조)
mysqldump -u root -p --no-data lms_local > backup_schema.sql

# 데이터만 백업 (INSERT 문)
mysqldump -u root -p --no-create-info lms_local > backup_data.sql

# 특정 테이블만 백업
mysqldump -u root -p lms_local users course lesson > backup_partial.sql
```

### 3.2 데이터 복원 (Import)

```bash
# 먼저 대상 DB 생성
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS lms_local CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 백업 파일 복원
mysql -u root -p lms_local < backup_full.sql
```

### 3.3 클라우드 DB로 복원

```bash
# Railway 예시
mysql -h containers-us-west-xxx.railway.app -P 6789 -u root -p railway < backup_full.sql

# AWS RDS 예시
mysql -h your-instance.region.rds.amazonaws.com -P 3306 -u admin -p lms_prod < backup_full.sql
```

---

## 4. GUI 툴 사용 방법

### 4.1 DBeaver (추천)

**백업 (Export)**
1. 데이터베이스 우클릭 → "Tools" → "Dump Database"
2. 출력 폴더 선택
3. "Start" 클릭

**복원 (Import)**
1. 대상 데이터베이스 우클릭 → "Tools" → "Execute Script"
2. 백업 SQL 파일 선택
3. "Start" 클릭

### 4.2 HeidiSQL

**백업 (Export)**
1. 도구 → "데이터베이스 SQL로 내보내기"
2. 테이블 선택
3. 옵션 설정:
   - "Create database" 체크
   - "Create table" 체크
   - "Data" 체크
4. 파일로 저장

**복원 (Import)**
1. 파일 → "SQL 파일 실행"
2. 백업 파일 선택
3. 실행

### 4.3 MySQL Workbench

**백업 (Export)**
1. Server → Data Export
2. 데이터베이스 선택
3. "Export to Self-Contained File" 선택
4. "Start Export"

**복원 (Import)**
1. Server → Data Import
2. "Import from Self-Contained File" 선택
3. SQL 파일 선택
4. "Start Import"

---

## 5. 클라우드 DB 설정 가이드

### 5.1 Railway (가장 쉬움, 추천)

1. https://railway.app 접속, GitHub 로그인
2. "New Project" → "Provision MySQL"
3. 생성된 DB 클릭 → "Variables" 탭
4. 연결 정보 확인:
   - `MYSQL_HOST`
   - `MYSQL_PORT`
   - `MYSQL_DATABASE`
   - `MYSQL_USER`
   - `MYSQL_PASSWORD`
5. `application-prod.yml` 수정:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}
       username: ${MYSQL_USER}
       password: ${MYSQL_PASSWORD}
   ```

### 5.2 PlanetScale

1. https://planetscale.com 접속, GitHub 로그인
2. "Create a new database" → 리전 선택 (Tokyo 추천)
3. "Connect" → "Create password"
4. Connection string 복사
5. `application-prod.yml` 수정

### 5.3 AWS RDS

1. AWS 콘솔 → RDS → "데이터베이스 생성"
2. MariaDB 선택, 프리 티어 선택
3. DB 인스턴스 설정:
   - DB 이름: `lms_prod`
   - 마스터 사용자 이름: `admin`
   - 마스터 암호 설정
4. 연결 설정:
   - "퍼블릭 액세스": 예 (개발 중에만)
   - 보안 그룹: 인바운드 3306 포트 열기
5. 엔드포인트 주소로 연결

---

## 6. 이전 후 검증 체크리스트

- [ ] 애플리케이션 정상 시작 확인
- [ ] 로그인 테스트 (admin/admin123)
- [ ] 강좌 목록 조회 확인
- [ ] 수강 신청 테스트
- [ ] 레코드 수 비교 (원본 vs 이전 후)
  ```sql
  SELECT 'users' AS tbl, COUNT(*) FROM users
  UNION ALL
  SELECT 'course', COUNT(*) FROM course
  UNION ALL
  SELECT 'lesson', COUNT(*) FROM lesson
  UNION ALL
  SELECT 'course_enrollment', COUNT(*) FROM course_enrollment;
  ```
- [ ] 한글 데이터 깨짐 없음 확인
- [ ] 외래 키 제약조건 정상 동작 확인

---

## 7. 문제 해결

### 한글 깨짐
```sql
-- 데이터베이스 인코딩 확인
SHOW CREATE DATABASE lms_local;

-- 인코딩 변경 (필요시)
ALTER DATABASE lms_local CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 접속 거부 (Access Denied)
- 사용자 권한 확인
- IP 허용 목록 확인 (클라우드의 경우)
- 방화벽 설정 확인

### 외래 키 에러
```sql
-- 외래 키 검사 비활성화 후 임포트
SET FOREIGN_KEY_CHECKS = 0;
-- ... import ...
SET FOREIGN_KEY_CHECKS = 1;
```

### 대용량 파일 임포트 실패
```bash
# max_allowed_packet 증가
mysql -u root -p --max_allowed_packet=512M lms_local < backup_full.sql
```

---

## 8. 정기 백업 권장 일정

| 주기 | 대상 | 파일명 예시 |
|------|------|-------------|
| 매일 | 전체 데이터 | `backup_20260126_full.sql` |
| 매주 | 스키마 | `schema_week04.sql` |
| 마일스톤 | 전체 + 코드 | `milestone_v1.0_full.sql` |

---

## 9. 백업 자동화 스크립트 (Windows)

`backup.bat` 파일 생성:
```batch
@echo off
set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%
set BACKUP_DIR=C:\lms-backups
set DB_NAME=lms_local
set DB_USER=root
set DB_PASS=1234

if not exist %BACKUP_DIR% mkdir %BACKUP_DIR%

mysqldump -u %DB_USER% -p%DB_PASS% %DB_NAME% > %BACKUP_DIR%\backup_%TIMESTAMP%.sql

echo Backup completed: %BACKUP_DIR%\backup_%TIMESTAMP%.sql
```

Windows 작업 스케줄러에 등록하면 자동 백업 가능.

---

## 문의

이전 과정에서 문제가 발생하면 팀 리드에게 연락하세요.
