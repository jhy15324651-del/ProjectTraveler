/**
 * 학습하기 목록 페이지 (learning.html) JavaScript
 * 향후 백엔드 연동 시 th:each 및 API 호출로 대체 가능
 */

document.addEventListener('DOMContentLoaded', function() {
    // 카테고리 카드 클릭 이벤트
    initCategoryCards();

    // 강의 카드 hover 효과 향상
    initCourseCards();
});

/**
 * 카테고리 카드 클릭 시 해당 카테고리로 필터링
 * 향후: /learning?category=language 형태로 URL 파라미터 처리
 */
function initCategoryCards() {
    const categoryCards = document.querySelectorAll('.learn-category-card');

    categoryCards.forEach(card => {
        card.addEventListener('click', function() {
            const category = this.dataset.category;
            filterCoursesByCategory(category);
        });
    });
}

/**
 * 카테고리별 강의 필터링
 * @param {string} category - 카테고리 이름 (language, culture, travel)
 */
function filterCoursesByCategory(category) {
    const courseCards = document.querySelectorAll('.learn-course-card');

    courseCards.forEach(card => {
        const cardCategory = card.dataset.category;

        if (category === 'all' || cardCategory === category) {
            card.style.display = 'flex';
            // 애니메이션 효과
            card.style.opacity = '0';
            card.style.transform = 'translateY(20px)';
            setTimeout(() => {
                card.style.transition = 'opacity 0.3s, transform 0.3s';
                card.style.opacity = '1';
                card.style.transform = 'translateY(0)';
            }, 50);
        } else {
            card.style.display = 'none';
        }
    });

    // 섹션 타이틀 업데이트
    updateSectionTitle(category);
}

/**
 * 섹션 타이틀 업데이트
 */
function updateSectionTitle(category) {
    const titleElement = document.querySelector('.learn-section-title');
    const titles = {
        'all': '추천 강의',
        'language': '일본어 강의',
        'culture': '문화 강의',
        'travel': '여행 강의'
    };

    if (titleElement && titles[category]) {
        titleElement.textContent = titles[category];
    }
}

/**
 * 강의 카드 초기화
 */
function initCourseCards() {
    const courseCards = document.querySelectorAll('.learn-course-card');

    courseCards.forEach(card => {
        // 진도에 따른 링크 텍스트 업데이트
        const progressIndicator = card.querySelector('.learn-course-progress-indicator');
        const linkText = card.querySelector('.learn-course-link');

        if (progressIndicator && linkText) {
            const progress = parseInt(progressIndicator.dataset.progress) || 0;

            if (progress === 0) {
                linkText.textContent = '강의 시작하기 >';
            } else if (progress >= 90) {
                linkText.textContent = '마무리하기 >';
            } else {
                linkText.textContent = '이어서 학습하기 >';
            }
        }
    });
}

/**
 * 강의 시작하기 (향후 사용)
 * @param {string} courseId - 강좌 ID
 */
function startCourse(courseId) {
    // 향후: 백엔드 API 호출 후 플레이어 페이지로 이동
    // window.location.href = `/learning/${courseId}/lessons/1`;
    console.log('Start course:', courseId);
    window.location.href = `course-detail.html?id=${courseId}`;
}

/**
 * 강의 이어하기 (향후 사용)
 * @param {string} courseId - 강좌 ID
 * @param {string} lessonId - 마지막 레슨 ID
 */
function continueCourse(courseId, lessonId) {
    // 향후: 백엔드에서 마지막 학습 위치 조회
    // window.location.href = `/learning/${courseId}/lessons/${lessonId}`;
    console.log('Continue course:', courseId, 'from lesson:', lessonId);
}