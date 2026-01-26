-- ================================================
-- LMS Travel Database Schema
-- MariaDB / MySQL 호환
-- ================================================
-- 실행 순서: 이 파일 전체를 순서대로 실행
-- 사용법: mysql -u root -p < schema.sql
-- ================================================

-- 데이터베이스 생성 (로컬용)
CREATE DATABASE IF NOT EXISTS lms_local
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 운영용 데이터베이스 (필요 시)
-- CREATE DATABASE IF NOT EXISTS lms_prod
--     CHARACTER SET utf8mb4
--     COLLATE utf8mb4_unicode_ci;

USE lms_local;

-- ================================================
-- 테이블 삭제 (재생성 시 사용, 순서 중요!)
-- ================================================
-- DROP TABLE IF EXISTS learning_session;
-- DROP TABLE IF EXISTS lesson_progress;
-- DROP TABLE IF EXISTS attendance_daily;
-- DROP TABLE IF EXISTS course_enrollment;
-- DROP TABLE IF EXISTS lesson;
-- DROP TABLE IF EXISTS course_unit;
-- DROP TABLE IF EXISTS course;
-- DROP TABLE IF EXISTS users;

-- ================================================
-- 1. 사용자 테이블 (users)
-- ================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '로그인 아이디',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt 암호화',
    email VARCHAR(100) COMMENT '이메일',
    full_name VARCHAR(100) COMMENT '이름',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'ADMIN / USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '계정 활성화 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 정보';

-- ================================================
-- 2. 강좌 테이블 (course)
-- ================================================
CREATE TABLE IF NOT EXISTS course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '강좌명',
    short_desc TEXT COMMENT '짧은 설명',
    full_desc TEXT COMMENT '상세 설명',
    thumbnail_url VARCHAR(500) COMMENT '썸네일 이미지 URL',
    category VARCHAR(50) COMMENT 'LANGUAGE / CULTURE / TRAVEL / FOOD',
    level VARCHAR(20) DEFAULT 'BEGINNER' COMMENT 'BEGINNER / INTERMEDIATE / ADVANCED',
    total_duration_sec INT DEFAULT 0 COMMENT '총 강의 시간(초)',
    enroll_policy VARCHAR(20) DEFAULT 'SELF' COMMENT 'SELF / APPROVAL / ASSIGN_ONLY',
    active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '강좌 활성화 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course_category (category),
    INDEX idx_course_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='강좌 정보';

-- ================================================
-- 3. 유닛(단원) 테이블 (course_unit)
-- ================================================
CREATE TABLE IF NOT EXISTS course_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL COMMENT '강좌 ID',
    title VARCHAR(200) NOT NULL COMMENT '유닛(단원) 제목',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    duration_sec INT DEFAULT 0 COMMENT '유닛 총 시간(초)',
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    INDEX idx_unit_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='강좌 유닛(단원)';

-- ================================================
-- 4. 레슨 테이블 (lesson)
-- ================================================
CREATE TABLE IF NOT EXISTS lesson (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL COMMENT '강좌 ID',
    unit_id BIGINT COMMENT '유닛 ID (없으면 강좌 직속)',
    title VARCHAR(200) NOT NULL COMMENT '레슨 제목',
    description TEXT COMMENT '레슨 설명',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    duration_sec INT DEFAULT 0 COMMENT '영상 길이(초)',
    video_type VARCHAR(20) DEFAULT 'NONE' COMMENT 'NONE / MP4 / YOUTUBE / HLS',
    video_url VARCHAR(1000) COMMENT '영상 URL 또는 경로',
    is_preview BOOLEAN DEFAULT FALSE COMMENT '미리보기 가능 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    FOREIGN KEY (unit_id) REFERENCES course_unit(id) ON DELETE SET NULL,
    INDEX idx_lesson_course (course_id),
    INDEX idx_lesson_unit (unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='레슨(강의)';

-- ================================================
-- 5. 수강 등록 테이블 (course_enrollment)
-- ================================================
CREATE TABLE IF NOT EXISTS course_enrollment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    course_id BIGINT NOT NULL COMMENT '강좌 ID',
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED' COMMENT 'REQUESTED / APPROVED / REJECTED / ASSIGNED / COMPLETED / PAUSED',
    source VARCHAR(20) NOT NULL DEFAULT 'USER_REQUEST' COMMENT 'USER_REQUEST / ADMIN_ASSIGN',
    requested_at DATETIME COMMENT '신청 일시',
    approved_at DATETIME COMMENT '승인 일시',
    assigned_at DATETIME COMMENT '배정 일시',
    assigned_by_admin_id BIGINT COMMENT '배정한 관리자 ID',
    note TEXT COMMENT '메모',
    last_lesson_id BIGINT COMMENT '마지막 학습 레슨 ID',
    last_accessed_at DATETIME COMMENT '마지막 접근 일시',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_enrollment (user_id, course_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by_admin_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (last_lesson_id) REFERENCES lesson(id) ON DELETE SET NULL,
    INDEX idx_enrollment_user (user_id),
    INDEX idx_enrollment_course (course_id),
    INDEX idx_enrollment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='수강 등록';

-- ================================================
-- 6. 레슨 진도 테이블 (lesson_progress)
-- ================================================
CREATE TABLE IF NOT EXISTS lesson_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    course_id BIGINT NOT NULL COMMENT '강좌 ID',
    lesson_id BIGINT NOT NULL COMMENT '레슨 ID',
    last_position_sec INT DEFAULT 0 COMMENT '마지막 재생 위치(초) - 이어보기용',
    watched_sec INT DEFAULT 0 COMMENT '누적 시청 시간(초) - 완료 판정용',
    completed BOOLEAN NOT NULL DEFAULT FALSE COMMENT '완료 여부',
    completed_at DATETIME COMMENT '완료 일시',
    started_at DATETIME COMMENT '학습 시작 일시',
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_progress (user_id, lesson_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    FOREIGN KEY (lesson_id) REFERENCES lesson(id) ON DELETE CASCADE,
    INDEX idx_progress_user (user_id),
    INDEX idx_progress_course (course_id),
    INDEX idx_progress_lesson (lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='레슨 진도';

-- ================================================
-- 7. 학습 세션 테이블 (learning_session)
-- ================================================
CREATE TABLE IF NOT EXISTS learning_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    course_id BIGINT COMMENT '강좌 ID',
    lesson_id BIGINT COMMENT '레슨 ID',
    started_at DATETIME NOT NULL COMMENT '세션 시작',
    ended_at DATETIME COMMENT '세션 종료',
    duration_sec INT COMMENT '세션 시간(초)',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE SET NULL,
    FOREIGN KEY (lesson_id) REFERENCES lesson(id) ON DELETE SET NULL,
    INDEX idx_session_user (user_id),
    INDEX idx_session_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='학습 세션';

-- ================================================
-- 8. 일일 출석 테이블 (attendance_daily)
-- ================================================
CREATE TABLE IF NOT EXISTS attendance_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    attend_date DATE NOT NULL COMMENT '출석 날짜',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_attendance (user_id, attend_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_attendance_user (user_id),
    INDEX idx_attendance_date (attend_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='일일 출석';

-- ================================================
-- 스키마 생성 완료!
-- 다음 단계: seed.sql 실행하여 초기 데이터 추가
-- ================================================
