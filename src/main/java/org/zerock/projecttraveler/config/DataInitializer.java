package org.zerock.projecttraveler.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.projecttraveler.entity.*;
import org.zerock.projecttraveler.repository.*;
import org.zerock.projecttraveler.service.UserService;

import java.util.List;

/**
 * 초기 데이터 생성 (개발/데모용)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseUnitRepository unitRepository;
    private final LessonRepository lessonRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 데이터가 있으면 스킵
        if (userRepository.count() > 0) {
            log.info("데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("초기 데이터 생성 시작...");

        // 1. 관리자 계정 생성
        User admin = userService.registerAdmin("admin", "admin123", "admin@lms.com", "관리자");
        log.info("관리자 계정 생성: admin / admin123");

        // 2. 테스트 사용자 생성
        User user1 = userService.register("user1", "user123", "user1@test.com", "홍길동");
        User user2 = userService.register("user2", "user123", "user2@test.com", "김철수");
        log.info("테스트 사용자 생성: user1, user2 / user123");

        // 3. 샘플 강좌 생성
        createSampleCourses();

        log.info("초기 데이터 생성 완료!");
    }

    private void createSampleCourses() {
        // 강좌 1: 일본어 기초 회화
        Course course1 = courseRepository.save(Course.builder()
                .title("일본어 기초 회화")
                .shortDesc("여행에 필요한 기본 일본어 회화를 배워보세요. 인사, 주문, 길 찾기 등 실용적인 표현을 학습합니다.")
                .fullDesc("이 강좌는 일본 여행을 준비하는 분들을 위한 기초 일본어 회화 과정입니다. 실제 여행 상황에서 바로 사용할 수 있는 실용적인 표현들을 중심으로 구성되어 있습니다.")
                .thumbnailUrl("https://images.unsplash.com/photo-1528164344705-47542687000d?w=800")
                .category(Course.Category.LANGUAGE)
                .level(Course.Level.BEGINNER)
                .enrollPolicy(Course.EnrollPolicy.SELF)
                .totalDurationSec(10800) // 3시간
                .build());

        // 유닛 1
        CourseUnit unit1_1 = unitRepository.save(CourseUnit.builder()
                .course(course1).title("기본 인사말").sortOrder(1).build());

        lessonRepository.saveAll(List.of(
                Lesson.builder().course(course1).unit(unit1_1).title("こんにちは - 인사하기").sortOrder(1).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course1).unit(unit1_1).title("はじめまして - 자기소개").sortOrder(2).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course1).unit(unit1_1).title("さようなら - 작별 인사").sortOrder(3).durationSec(900).videoType(Lesson.VideoType.NONE).build()
        ));

        // 유닛 2
        CourseUnit unit1_2 = unitRepository.save(CourseUnit.builder()
                .course(course1).title("숫자와 가격").sortOrder(2).build());

        lessonRepository.saveAll(List.of(
                Lesson.builder().course(course1).unit(unit1_2).title("일본어 숫자 1~10").sortOrder(4).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course1).unit(unit1_2).title("가격 묻기와 답하기").sortOrder(5).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course1).unit(unit1_2).title("쇼핑 실전 회화").sortOrder(6).durationSec(900).videoType(Lesson.VideoType.NONE).build()
        ));

        // 유닛 3
        CourseUnit unit1_3 = unitRepository.save(CourseUnit.builder()
                .course(course1).title("길 찾기와 교통").sortOrder(3).build());

        lessonRepository.saveAll(List.of(
                Lesson.builder().course(course1).unit(unit1_3).title("길 묻기 기본 표현").sortOrder(7).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course1).unit(unit1_3).title("전철/버스 이용하기").sortOrder(8).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course1).unit(unit1_3).title("택시 이용 회화").sortOrder(9).durationSec(900).videoType(Lesson.VideoType.NONE).build()
        ));

        // 유닛 4
        CourseUnit unit1_4 = unitRepository.save(CourseUnit.builder()
                .course(course1).title("음식점에서").sortOrder(4).build());

        lessonRepository.saveAll(List.of(
                Lesson.builder().course(course1).unit(unit1_4).title("메뉴 주문하기").sortOrder(10).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course1).unit(unit1_4).title("음식 관련 표현").sortOrder(11).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course1).unit(unit1_4).title("계산하기").sortOrder(12).durationSec(900).videoType(Lesson.VideoType.NONE).build()
        ));

        // 강좌 2: 일본어 중급
        Course course2 = courseRepository.save(Course.builder()
                .title("일본어 중급")
                .shortDesc("일본어 기초를 넘어 더 깊이있는 대화를 할 수 있도록 중급 문법과 표현을 학습합니다.")
                .thumbnailUrl("https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800")
                .category(Course.Category.LANGUAGE)
                .level(Course.Level.INTERMEDIATE)
                .enrollPolicy(Course.EnrollPolicy.APPROVAL)
                .totalDurationSec(14400) // 4시간
                .build());

        CourseUnit unit2_1 = unitRepository.save(CourseUnit.builder()
                .course(course2).title("중급 문법").sortOrder(1).build());

        lessonRepository.saveAll(List.of(
                Lesson.builder().course(course2).unit(unit2_1).title("존경어의 기초").sortOrder(1).durationSec(1200).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course2).unit(unit2_1).title("겸양어 사용법").sortOrder(2).durationSec(1200).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course2).unit(unit2_1).title("복합 문장 만들기").sortOrder(3).durationSec(1200).videoType(Lesson.VideoType.NONE).build()
        ));

        // 강좌 3: 일본 문화 이해
        Course course3 = courseRepository.save(Course.builder()
                .title("일본 문화 이해")
                .shortDesc("일본의 전통 문화, 예절, 관습 등을 배워 더 깊이있는 여행을 준비하세요.")
                .thumbnailUrl("https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=800")
                .category(Course.Category.CULTURE)
                .level(Course.Level.BEGINNER)
                .enrollPolicy(Course.EnrollPolicy.SELF)
                .totalDurationSec(9000) // 2.5시간
                .build());

        CourseUnit unit3_1 = unitRepository.save(CourseUnit.builder()
                .course(course3).title("일본 전통 문화").sortOrder(1).build());

        lessonRepository.saveAll(List.of(
                Lesson.builder().course(course3).unit(unit3_1).title("다도의 역사와 예절").sortOrder(1).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course3).unit(unit3_1).title("기모노의 종류와 착용법").sortOrder(2).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course3).unit(unit3_1).title("일본의 전통 축제").sortOrder(3).durationSec(900).videoType(Lesson.VideoType.NONE).build()
        ));

        // 강좌 4: 도쿄 여행 가이드
        Course course4 = courseRepository.save(Course.builder()
                .title("도쿄 여행 가이드")
                .shortDesc("도쿄의 주요 명소, 교통수단, 맛집 등 도쿄 여행에 필요한 모든 정보를 학습합니다.")
                .thumbnailUrl("https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800")
                .category(Course.Category.TRAVEL)
                .level(Course.Level.BEGINNER)
                .enrollPolicy(Course.EnrollPolicy.SELF)
                .totalDurationSec(7200) // 2시간
                .build());

        CourseUnit unit4_1 = unitRepository.save(CourseUnit.builder()
                .course(course4).title("도쿄 핵심 명소").sortOrder(1).build());

        lessonRepository.saveAll(List.of(
                Lesson.builder().course(course4).unit(unit4_1).title("신주쿠/시부야 탐방").sortOrder(1).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course4).unit(unit4_1).title("아사쿠사/우에노 가이드").sortOrder(2).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course4).unit(unit4_1).title("하라주쿠/오모테산도").sortOrder(3).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course4).unit(unit4_1).title("오다이바/도쿄타워").sortOrder(4).durationSec(900).videoType(Lesson.VideoType.NONE).build()
        ));

        // 강좌 5: 일본 교통수단 이용법
        Course course5 = courseRepository.save(Course.builder()
                .title("일본 교통수단 이용법")
                .shortDesc("일본의 전철, 버스, 택시 등 다양한 교통수단의 이용 방법을 배워보세요.")
                .thumbnailUrl("https://images.unsplash.com/photo-1590559899731-a382839e5549?w=800")
                .category(Course.Category.TRAVEL)
                .level(Course.Level.BEGINNER)
                .enrollPolicy(Course.EnrollPolicy.SELF)
                .totalDurationSec(5400) // 1.5시간
                .build());

        CourseUnit unit5_1 = unitRepository.save(CourseUnit.builder()
                .course(course5).title("대중교통 마스터").sortOrder(1).build());

        lessonRepository.saveAll(List.of(
                Lesson.builder().course(course5).unit(unit5_1).title("JR 패스 활용법").sortOrder(1).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course5).unit(unit5_1).title("지하철 노선 이해").sortOrder(2).durationSec(900).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course5).unit(unit5_1).title("IC 카드 사용법").sortOrder(3).durationSec(900).videoType(Lesson.VideoType.NONE).build()
        ));

        // 강좌 6: 일본 음식 문화
        Course course6 = courseRepository.save(Course.builder()
                .title("일본 음식 문화")
                .shortDesc("일본의 대표 음식, 식사 예절, 맛집 찾는 법 등을 학습하여 일본 음식을 더 즐겨보세요.")
                .thumbnailUrl("https://images.unsplash.com/photo-1555400038-63f5ba517a47?w=800")
                .category(Course.Category.CULTURE)
                .level(Course.Level.BEGINNER)
                .enrollPolicy(Course.EnrollPolicy.SELF)
                .totalDurationSec(7200) // 2시간
                .build());

        CourseUnit unit6_1 = unitRepository.save(CourseUnit.builder()
                .course(course6).title("일본 음식의 세계").sortOrder(1).build());

        lessonRepository.saveAll(List.of(
                Lesson.builder().course(course6).unit(unit6_1).title("스시의 종류와 먹는 법").sortOrder(1).durationSec(800).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course6).unit(unit6_1).title("라멘 가이드").sortOrder(2).durationSec(800).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course6).unit(unit6_1).title("이자카야 이용법").sortOrder(3).durationSec(800).videoType(Lesson.VideoType.NONE).build(),
                Lesson.builder().course(course6).unit(unit6_1).title("편의점 음식 추천").sortOrder(4).durationSec(800).videoType(Lesson.VideoType.NONE).build()
        ));

        log.info("샘플 강좌 6개 생성 완료");
    }
}
