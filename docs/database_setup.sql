-- ================================================
-- LMS Database Setup Script
-- MariaDB / MySQL
-- ================================================

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS lms_travel
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE lms_travel;

-- ================================================
-- 참고: Spring JPA가 ddl-auto=update로 자동 생성하므로
-- 아래 테이블 생성문은 참고용입니다.
-- 직접 실행할 필요는 없습니다.
-- ================================================

-- 1. 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 강좌 테이블
CREATE TABLE IF NOT EXISTS course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    short_desc TEXT,
    full_desc TEXT,
    thumbnail_url VARCHAR(500),
    category VARCHAR(50),
    level VARCHAR(20) DEFAULT 'BEGINNER',
    total_duration_sec INT DEFAULT 0,
    enroll_policy VARCHAR(20) DEFAULT 'SELF',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 유닛(단원) 테이블
CREATE TABLE IF NOT EXISTS course_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    duration_sec INT DEFAULT 0,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 레슨 테이블
CREATE TABLE IF NOT EXISTS lesson (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    unit_id BIGINT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    duration_sec INT DEFAULT 0,
    video_type VARCHAR(20) DEFAULT 'NONE',
    video_url VARCHAR(1000),
    is_preview BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    FOREIGN KEY (unit_id) REFERENCES course_unit(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 수강 등록 테이블
CREATE TABLE IF NOT EXISTS course_enrollment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    source VARCHAR(20) NOT NULL DEFAULT 'USER_REQUEST',
    requested_at DATETIME,
    approved_at DATETIME,
    assigned_at DATETIME,
    assigned_by_admin_id BIGINT,
    note TEXT,
    last_lesson_id BIGINT,
    last_accessed_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE KEY uk_enrollment (user_id, course_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by_admin_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (last_lesson_id) REFERENCES lesson(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 레슨 진도 테이블
CREATE TABLE IF NOT EXISTS lesson_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    last_position_sec INT DEFAULT 0,
    watched_sec INT DEFAULT 0,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at DATETIME,
    started_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_progress (user_id, lesson_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    FOREIGN KEY (lesson_id) REFERENCES lesson(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 학습 세션 테이블
CREATE TABLE IF NOT EXISTS learning_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT,
    lesson_id BIGINT,
    started_at DATETIME NOT NULL,
    ended_at DATETIME,
    duration_sec INT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE SET NULL,
    FOREIGN KEY (lesson_id) REFERENCES lesson(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 일일 출석 테이블
CREATE TABLE IF NOT EXISTS attendance_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    attend_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_attendance (user_id, attend_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ================================================
-- 인덱스 추가 (성능 최적화)
-- ================================================

CREATE INDEX idx_enrollment_user ON course_enrollment(user_id);
CREATE INDEX idx_enrollment_status ON course_enrollment(status);
CREATE INDEX idx_progress_user ON lesson_progress(user_id);
CREATE INDEX idx_progress_course ON lesson_progress(course_id);
CREATE INDEX idx_attendance_user ON attendance_daily(user_id);
CREATE INDEX idx_attendance_date ON attendance_daily(attend_date);
