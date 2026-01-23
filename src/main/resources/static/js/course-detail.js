/**
 * 강좌 상세/커리큘럼 페이지 (course-detail.html) JavaScript
 * 탭 전환, 유닛 토글, 레슨 상태 관리
 */

document.addEventListener('DOMContentLoaded', function() {
    // 탭 네비게이션 초기화
    initTabs();

    // 유닛 토글 초기화
    initUnitToggle();

    // 레슨 클릭 이벤트
    initLessonItems();

    // 학습 시작/이어하기 버튼
    initActionButtons();
});

/**
 * 탭 네비게이션 초기화
 */
function initTabs() {
    const tabs = document.querySelectorAll('.course-tab');
    const tabContents = document.querySelectorAll('.course-tab-content');

    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const targetTab = this.dataset.tab;

            // 모든 탭 비활성화
            tabs.forEach(t => t.classList.remove('is-active'));
            tabContents.forEach(c => c.classList.remove('is-active'));

            // 클릭한 탭 활성화
            this.classList.add('is-active');

            // 해당 컨텐츠 표시
            const targetContent = document.querySelector(`[data-tab-content="${targetTab}"]`);
            if (targetContent) {
                targetContent.classList.add('is-active');
            }
        });
    });
}

/**
 * 유닛 토글 초기화
 */
function initUnitToggle() {
    const unitHeaders = document.querySelectorAll('.course-unit-header');

    unitHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const unit = this.closest('.course-unit');
            const lessons = unit.querySelector('.course-unit-lessons');
            const toggle = this.querySelector('.course-unit-toggle');

            if (lessons) {
                // 토글 상태 전환
                lessons.classList.toggle('is-open');

                // 아이콘 회전
                if (lessons.classList.contains('is-open')) {
                    toggle.style.transform = 'rotate(180deg)';
                } else {
                    toggle.style.transform = 'rotate(0deg)';
                }
            }
        });
    });
}

/**
 * 레슨 아이템 클릭 이벤트
 */
function initLessonItems() {
    const lessonItems = document.querySelectorAll('.course-lesson-item');

    lessonItems.forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();

            const lessonId = this.dataset.lessonId;
            const isLocked = this.classList.contains('is-locked');

            if (isLocked) {
                // 잠긴 레슨 클릭 시 알림
                showLockedMessage();
                return;
            }

            // 레슨 플레이어로 이동
            // 향후: /learning/{courseId}/lessons/{lessonId} 형태로 이동
            goToLesson(lessonId);
        });
    });
}

/**
 * 레슨 플레이어로 이동
 * @param {string} lessonId - 레슨 ID
 */
function goToLesson(lessonId) {
    // 향후: 실제 플레이어 페이지 구현 시 활성화
    // const courseId = getCourseIdFromUrl();
    // window.location.href = `/learning/${courseId}/lessons/${lessonId}`;

    console.log('Go to lesson:', lessonId);

    // 임시: 알림 표시
    showComingSoonMessage('레슨 플레이어');
}

/**
 * 잠긴 레슨 메시지 표시
 */
function showLockedMessage() {
    alert('이전 레슨을 먼저 완료해주세요.');
}

/**
 * 준비 중 메시지 표시
 * @param {string} featureName - 기능 이름
 */
function showComingSoonMessage(featureName) {
    alert(`${featureName} 기능은 준비 중입니다.`);
}

/**
 * 학습 시작/이어하기 버튼 초기화
 */
function initActionButtons() {
    const startButton = document.querySelector('.btn-course-start');
    const restartButton = document.querySelector('.btn-course-secondary');

    if (startButton) {
        startButton.addEventListener('click', function(e) {
            e.preventDefault();

            const action = this.dataset.action;
            const lessonId = this.dataset.lessonId;

            if (action === 'continue') {
                // 이어서 학습하기
                goToLesson(lessonId);
            } else {
                // 처음부터 시작
                goToLesson('1');
            }
        });
    }

    if (restartButton) {
        restartButton.addEventListener('click', function() {
            if (confirm('처음부터 다시 시작하시겠습니까? 기존 진도는 유지됩니다.')) {
                goToLesson('1');
            }
        });
    }
}

/**
 * URL에서 강좌 ID 추출
 * @returns {string|null} 강좌 ID
 */
function getCourseIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get('id');
}

/**
 * 레슨 완료 처리 (향후 사용)
 * @param {string} lessonId - 완료된 레슨 ID
 */
function markLessonComplete(lessonId) {
    // 향후: 백엔드 API 호출
    // POST /api/lessons/{lessonId}/complete

    const lessonItem = document.querySelector(`[data-lesson-id="${lessonId}"]`);
    if (lessonItem) {
        lessonItem.classList.remove('is-current');
        lessonItem.classList.add('is-done');

        // 완료 아이콘으로 변경
        const statusEl = lessonItem.querySelector('.course-lesson-status');
        if (statusEl) {
            statusEl.innerHTML = `
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                    <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
            `;
        }
    }

    // 다음 레슨 활성화
    activateNextLesson(lessonId);

    // 진도율 업데이트
    updateProgress();
}

/**
 * 다음 레슨 활성화 (향후 사용)
 * @param {string} currentLessonId - 현재 완료된 레슨 ID
 */
function activateNextLesson(currentLessonId) {
    const nextLessonId = parseInt(currentLessonId) + 1;
    const nextLesson = document.querySelector(`[data-lesson-id="${nextLessonId}"]`);

    if (nextLesson) {
        nextLesson.classList.remove('is-locked');
        nextLesson.classList.add('is-current');

        // 학습중 배지 추가
        const infoEl = nextLesson.querySelector('.course-lesson-info');
        if (infoEl && !infoEl.querySelector('.course-lesson-badge')) {
            const badge = document.createElement('span');
            badge.className = 'course-lesson-badge';
            badge.textContent = '학습중';
            infoEl.appendChild(badge);
        }
    }
}

/**
 * 진도율 업데이트 (향후 사용)
 */
function updateProgress() {
    const totalLessons = document.querySelectorAll('.course-lesson-item').length;
    const completedLessons = document.querySelectorAll('.course-lesson-item.is-done').length;

    const progressPercent = Math.round((completedLessons / totalLessons) * 100);

    // 진도율 표시 업데이트
    const progressFill = document.querySelector('.course-progress-fill');
    const progressPercentText = document.querySelector('.course-progress-percent');
    const progressText = document.querySelector('.course-progress-text');

    if (progressFill) {
        progressFill.style.width = `${progressPercent}%`;
    }

    if (progressPercentText) {
        progressPercentText.textContent = `${progressPercent}%`;
    }

    if (progressText) {
        progressText.textContent = `${totalLessons}개 레슨 중 ${completedLessons}개 완료`;
    }
}