-- ================================================
-- LMS Travel Seed Data (초기 데이터)
-- MariaDB / MySQL 호환
-- ================================================
-- 실행 전제: schema.sql 이 먼저 실행되어 있어야 함
-- 사용법: mysql -u root -p lms_local < seed.sql
-- ================================================

USE lms_local;

-- ================================================
-- 1. 사용자 데이터
-- ================================================
-- 비밀번호는 BCrypt로 암호화됨
-- admin123 → $2a$10$... (실제 해시값)
-- user123 → $2a$10$... (실제 해시값)

INSERT INTO users (username, password, email, full_name, role, enabled, created_at) VALUES
-- 관리자 계정 (admin / admin123)
('admin', '$2a$10$8K1p/a0dL1LXMw0vR3h8oO8Q6Wd1YxjP5eQz8vX0hZg5Q9u8RfZdO', 'admin@lms.com', '관리자', 'ADMIN', TRUE, NOW()),
-- 일반 사용자 계정 (user1 / user123)
('user1', '$2a$10$N9qo8uLOickgx2ZMRZoMyu6pxQzLzf/FqnlX8qFzJ9JYwZ3/Wm3K6', 'user1@test.com', '김학습', 'USER', TRUE, NOW()),
-- 추가 테스트 사용자
('user2', '$2a$10$N9qo8uLOickgx2ZMRZoMyu6pxQzLzf/FqnlX8qFzJ9JYwZ3/Wm3K6', 'user2@test.com', '이테스트', 'USER', TRUE, NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ================================================
-- 2. 강좌 데이터
-- ================================================
INSERT INTO course (title, short_desc, full_desc, thumbnail_url, category, level, total_duration_sec, enroll_policy, active, created_at) VALUES
-- SELF 정책 (즉시 수강 가능)
('일본어 기초 회화',
 '여행에서 바로 쓸 수 있는 기초 일본어',
 '일본 여행 시 필수적인 기초 회화를 배웁니다. 인사, 쇼핑, 식당 주문 등 실용적인 표현을 학습합니다.',
 '/images/course-japanese.jpg',
 'LANGUAGE', 'BEGINNER', 3600, 'SELF', TRUE, NOW()),

-- APPROVAL 정책 (승인 필요)
('일본 문화 심화 과정',
 '일본의 역사와 문화를 깊이 이해하기',
 '일본의 역사, 전통, 현대 문화를 심도 있게 학습합니다. 여행의 깊이를 더해줄 교양 강좌입니다.',
 '/images/course-culture.jpg',
 'CULTURE', 'INTERMEDIATE', 5400, 'APPROVAL', TRUE, NOW()),

-- ASSIGN_ONLY 정책 (관리자 배정만)
('신입 교육 - 일본 여행 안전 수칙',
 '필수 이수 교육',
 '일본 여행 시 알아야 할 안전 수칙과 비상 상황 대처법을 배웁니다.',
 '/images/course-safety.jpg',
 'TRAVEL', 'BEGINNER', 1800, 'ASSIGN_ONLY', TRUE, NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ================================================
-- 3. 유닛(단원) 데이터
-- ================================================
-- 일본어 기초 회화 강좌의 유닛
INSERT INTO course_unit (course_id, title, sort_order, duration_sec) VALUES
(1, '1단원: 기본 인사', 1, 900),
(1, '2단원: 숫자와 시간', 2, 900),
(1, '3단원: 쇼핑 표현', 3, 900),
(1, '4단원: 식당 주문', 4, 900);

-- 일본 문화 심화 과정의 유닛
INSERT INTO course_unit (course_id, title, sort_order, duration_sec) VALUES
(2, '1장: 일본의 역사', 1, 1800),
(2, '2장: 전통 문화', 2, 1800),
(2, '3장: 현대 일본', 3, 1800);

-- ================================================
-- 4. 레슨 데이터
-- ================================================
-- 일본어 기초 회화 - 1단원 레슨
INSERT INTO lesson (course_id, unit_id, title, description, sort_order, duration_sec, video_type, video_url, is_preview, created_at) VALUES
(1, 1, '안녕하세요 (こんにちは)', '기본 인사말을 배웁니다', 1, 300, 'NONE', NULL, TRUE, NOW()),
(1, 1, '감사합니다 (ありがとう)', '감사 표현을 배웁니다', 2, 300, 'NONE', NULL, FALSE, NOW()),
(1, 1, '실례합니다 (すみません)', '양해를 구하는 표현', 3, 300, 'NONE', NULL, FALSE, NOW());

-- 일본어 기초 회화 - 2단원 레슨
INSERT INTO lesson (course_id, unit_id, title, description, sort_order, duration_sec, video_type, video_url, is_preview, created_at) VALUES
(1, 2, '숫자 1-10', '기본 숫자를 배웁니다', 1, 300, 'NONE', NULL, FALSE, NOW()),
(1, 2, '시간 표현', '시간을 말하는 법', 2, 300, 'NONE', NULL, FALSE, NOW()),
(1, 2, '날짜 표현', '날짜를 말하는 법', 3, 300, 'NONE', NULL, FALSE, NOW());

-- 일본어 기초 회화 - 3단원 레슨
INSERT INTO lesson (course_id, unit_id, title, description, sort_order, duration_sec, video_type, video_url, is_preview, created_at) VALUES
(1, 3, '이것은 얼마예요?', '가격 묻기', 1, 300, 'NONE', NULL, FALSE, NOW()),
(1, 3, '이것을 주세요', '물건 구매하기', 2, 300, 'NONE', NULL, FALSE, NOW()),
(1, 3, '카드 결제 가능해요?', '결제 방법 문의', 3, 300, 'NONE', NULL, FALSE, NOW());

-- 일본어 기초 회화 - 4단원 레슨
INSERT INTO lesson (course_id, unit_id, title, description, sort_order, duration_sec, video_type, video_url, is_preview, created_at) VALUES
(1, 4, '메뉴판 읽기', '일본어 메뉴 이해하기', 1, 300, 'NONE', NULL, FALSE, NOW()),
(1, 4, '주문하기', '음식 주문 표현', 2, 300, 'NONE', NULL, FALSE, NOW()),
(1, 4, '계산하기', '계산 요청 표현', 3, 300, 'NONE', NULL, FALSE, NOW());

-- 일본 문화 심화 과정 레슨
INSERT INTO lesson (course_id, unit_id, title, description, sort_order, duration_sec, video_type, video_url, is_preview, created_at) VALUES
(2, 5, '고대 일본', '조몬 시대부터 헤이안 시대', 1, 600, 'NONE', NULL, TRUE, NOW()),
(2, 5, '무사의 시대', '가마쿠라, 무로마치, 에도', 2, 600, 'NONE', NULL, FALSE, NOW()),
(2, 5, '근현대 일본', '메이지 유신부터 현재', 3, 600, 'NONE', NULL, FALSE, NOW()),
(2, 6, '다도와 꽃꽂이', '일본 전통 예술', 1, 600, 'NONE', NULL, FALSE, NOW()),
(2, 6, '마츠리 문화', '일본의 축제', 2, 600, 'NONE', NULL, FALSE, NOW()),
(2, 6, '전통 의상', '기모노의 세계', 3, 600, 'NONE', NULL, FALSE, NOW()),
(2, 7, '애니메이션과 만화', '일본 서브컬처', 1, 600, 'NONE', NULL, FALSE, NOW()),
(2, 7, '음식 문화', '라멘, 스시 그리고 이자카야', 2, 600, 'NONE', NULL, FALSE, NOW()),
(2, 7, '여행 트렌드', '요즘 일본 여행', 3, 600, 'NONE', NULL, FALSE, NOW());

-- 안전 수칙 강좌 레슨 (유닛 없이 직속)
INSERT INTO lesson (course_id, unit_id, title, description, sort_order, duration_sec, video_type, video_url, is_preview, created_at) VALUES
(3, NULL, '지진 대비', '지진 발생 시 행동 요령', 1, 600, 'NONE', NULL, FALSE, NOW()),
(3, NULL, '분실물 대처', '여권, 지갑 분실 시', 2, 600, 'NONE', NULL, FALSE, NOW()),
(3, NULL, '의료 응급상황', '병원 이용 방법', 3, 600, 'NONE', NULL, FALSE, NOW());

-- ================================================
-- 5. 샘플 수강 등록 (선택사항)
-- ================================================
-- user1이 일본어 기초 회화 수강 신청 (자동 승인)
INSERT INTO course_enrollment (user_id, course_id, status, source, requested_at, approved_at, created_at) VALUES
(2, 1, 'APPROVED', 'USER_REQUEST', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE status = 'APPROVED', approved_at = NOW();

-- ================================================
-- 시드 데이터 추가 완료!
-- ================================================
-- 로그인 정보:
--   관리자: admin / admin123
--   사용자: user1 / user123, user2 / user123
-- ================================================

SELECT '✅ Seed data inserted successfully!' AS result;
SELECT CONCAT('Users: ', COUNT(*)) AS count FROM users;
SELECT CONCAT('Courses: ', COUNT(*)) AS count FROM course;
SELECT CONCAT('Lessons: ', COUNT(*)) AS count FROM lesson;
