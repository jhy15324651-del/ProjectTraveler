/**
 * 강좌 상세/커리큘럼 페이지 (course-detail.html) JavaScript
 * 탭 전환, 유닛 토글, API 연동 (퀴즈/자료/Q&A/재수강)
 */

// DOM에서 읽는 런타임 값 (th:inline 대체)
let courseId;

document.addEventListener('DOMContentLoaded', function() {
    courseId = document.getElementById('courseId')?.value;

    // 탭 네비게이션 초기화
    initTabs();

    // 유닛 토글 초기화
    initUnitToggle();

    // 수강 가능한 경우 데이터 로드
    const isAccessible = document.getElementById('isAccessible')?.value === 'true';
    if (isAccessible) {
        loadQuizStatuses();
        loadResources();
        loadQna();
    } else {
        const resourcesEl = document.getElementById('resourcesLoading');
        if (resourcesEl) resourcesEl.innerHTML = `
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
            </svg>
            수강 신청 후 자료를 확인할 수 있습니다.`;

        const qnaWriteEl = document.getElementById('qnaWriteForm');
        if (qnaWriteEl) qnaWriteEl.style.display = 'none';

        const qnaLoadingEl = document.getElementById('qnaLoading');
        if (qnaLoadingEl) qnaLoadingEl.innerHTML = `
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            수강 신청 후 Q&A를 이용할 수 있습니다.`;
    }
});

// ============ UI 초기화 ============

/**
 * 탭 네비게이션 초기화
 */
function initTabs() {
    const tabs = document.querySelectorAll('.course-tab');
    const tabContents = document.querySelectorAll('.course-tab-content');

    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const targetTab = this.dataset.tab;

            tabs.forEach(t => t.classList.remove('is-active'));
            tabContents.forEach(c => c.classList.remove('is-active'));

            this.classList.add('is-active');

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
                lessons.classList.toggle('is-open');
                if (lessons.classList.contains('is-open')) {
                    toggle.style.transform = 'rotate(180deg)';
                } else {
                    toggle.style.transform = 'rotate(0deg)';
                }
            }
        });
    });
}

// ============ 수강 신청 ============

async function requestEnrollment() {
    const btn = document.getElementById('enrollBtn');
    btn.disabled = true;
    btn.textContent = '처리 중...';

    try {
        const response = await fetch('/api/enrollments/request', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ courseId: courseId })
        });

        const result = await response.json();

        if (result.success) {
            alert(result.message);
            location.reload();
        } else {
            alert(result.message);
            btn.disabled = false;
            btn.textContent = '수강 신청';
        }
    } catch (error) {
        alert('오류가 발생했습니다.');
        btn.disabled = false;
        btn.textContent = '수강 신청';
    }
}

// ============ 퀴즈 상태 로드 ============

/**
 * 퀴즈별 상태 로드 (여러 퀴즈 카드 각각에 대해, 사이클 인식)
 */
async function loadQuizStatuses() {
    const quizCards = document.querySelectorAll('.quiz-status-card');
    if (quizCards.length === 0) {
        loadQuizStatusLegacy();
        return;
    }

    for (const card of quizCards) {
        const quizId = card.getAttribute('data-quiz-id');
        try {
            const statusRes = await fetch(`/api/quiz/${quizId}/status`);
            const statusData = await statusRes.json();
            const status = statusData.success && statusData.data ? statusData.data : null;

            const actionArea = card.querySelector('.quiz-action-area');
            const iconEl = card.querySelector('div[style*="border-radius: 50%"]');

            if (!status) continue;

            if (status.quizStatusCode === 'PASSED') {
                iconEl.textContent = '\u2713';
                iconEl.style.background = '#e8f5e9';
                iconEl.style.color = '#2e7d32';
                card.querySelector('div[style*="font-size: 13px"]').textContent = '최고 점수: ' + status.bestScore + '점';
                actionArea.innerHTML = '<span style="padding: 8px 16px; background: #e8f5e9; color: #2e7d32; border-radius: 20px; font-size: 13px; font-weight: 600;">합격</span>';
            } else if (status.quizStatusCode === 'RETAKE_REQUIRED') {
                iconEl.textContent = '\uD83D\uDCDA';
                iconEl.style.background = '#fff3e0';
                card.querySelector('div[style*="font-size: 13px"]').textContent = '재수강이 필요합니다.';
                actionArea.innerHTML = `<button onclick="retakeCourse(${courseId})" style="padding: 10px 24px; background: #ff9800; color: white; border-radius: 8px; border: none; cursor: pointer; font-weight: 600; font-size: 14px;">재수강</button>`;
            } else if (status.quizStatusCode === 'RETRY_ALLOWED') {
                iconEl.style.background = '#fff3e0';
                card.querySelector('div[style*="font-size: 13px"]').textContent = '오답 확인 후 2차 시험에 응시하세요.';
                actionArea.innerHTML = `<a href="/quiz?courseId=${courseId}&quizId=${quizId}" style="padding: 10px 24px; background: var(--primary-red); color: white; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px;">퀴즈 응시</a>`;
            } else if (status.canAttempt) {
                actionArea.innerHTML = `<a href="/quiz?courseId=${courseId}&quizId=${quizId}" style="padding: 10px 24px; background: var(--primary-red); color: white; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px;">퀴즈 응시</a>`;
            } else {
                actionArea.innerHTML = '<span style="padding: 8px 16px; background: #f5f5f5; color: #666; border-radius: 20px; font-size: 13px;">응시 불가</span>';
            }
        } catch (e) {
            console.error('Quiz status load error for quizId=' + quizId, e);
        }
    }
}

/**
 * 단일 퀴즈 카드 fallback (레거시)
 */
async function loadQuizStatusLegacy() {
    try {
        const res = await fetch(`/api/quiz/status/${courseId}`);
        const data = await res.json();
        if (!data.success || !data.data) return;

        const status = data.data;
        const card = document.getElementById('quizStatusCard');
        const icon = document.getElementById('quizStatusIcon');
        const title = document.getElementById('quizStatusTitle');
        const msg = document.getElementById('quizStatusMessage');
        const action = document.getElementById('quizStatusAction');

        card.style.display = 'block';

        if (status.quizStatusCode === 'PASSED') {
            icon.textContent = '\u2713';
            icon.style.background = '#e8f5e9';
            icon.style.color = '#2e7d32';
            title.textContent = '퀴즈 합격 완료';
            msg.textContent = '최고 점수: ' + status.bestScore + '점';
            action.innerHTML = '<span style="padding: 8px 16px; background: #e8f5e9; color: #2e7d32; border-radius: 20px; font-size: 13px; font-weight: 600;">합격</span>';
        } else if (status.canAttempt) {
            icon.textContent = '\uD83D\uDCDD';
            title.textContent = status.title || '퀴즈';
            msg.textContent = status.message || '퀴즈에 응시할 수 있습니다.';
            action.innerHTML = `<a href="/quiz?courseId=${courseId}&quizId=${status.quizId}" style="padding: 10px 24px; background: var(--primary-red); color: white; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px;">퀴즈 응시</a>`;
        } else if (status.quizStatusCode === 'RETAKE_REQUIRED') {
            icon.textContent = '\uD83D\uDCDA';
            icon.style.background = '#fff3e0';
            title.textContent = '재수강 필요';
            msg.textContent = status.message || '강의를 다시 수강한 후 퀴즈에 응시해주세요.';
            action.innerHTML = `<button onclick="retakeCourse(${courseId})" style="padding: 10px 24px; background: #ff9800; color: white; border-radius: 8px; border: none; cursor: pointer; font-weight: 600; font-size: 14px;">재수강</button>`;
        } else if (status.quizStatusCode === 'RETRY_ALLOWED') {
            icon.textContent = '\uD83D\uDCDD';
            icon.style.background = '#fff3e0';
            title.textContent = '2차 시험 응시 가능';
            msg.textContent = '오답 확인 후 2차 시험에 응시하세요.';
            action.innerHTML = `<a href="/quiz?courseId=${courseId}&quizId=${status.quizId}" style="padding: 10px 24px; background: var(--primary-red); color: white; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px;">퀴즈 응시</a>`;
        } else {
            icon.textContent = '\uD83D\uDCDD';
            title.textContent = status.title || '퀴즈';
            msg.textContent = status.message || '';
        }
    } catch (e) {
        console.error('Quiz status load error:', e);
    }
}

// ============ 자료실 ============

async function loadResources() {
    try {
        const res = await fetch(`/api/course-resources/${courseId}`);
        const data = await res.json();
        const container = document.getElementById('resourcesContainer');

        if (!data.success || !data.data || data.data.length === 0) {
            container.innerHTML = `<p class="course-empty-message">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                </svg>
                등록된 학습 자료가 없습니다.</p>`;
            return;
        }

        let html = '';
        data.data.forEach(group => {
            html += `<div style="margin-bottom: 20px;">
                <h3 style="font-size: 16px; font-weight: 600; margin: 0 0 12px 0; padding: 10px 15px; background: #f8f9fa; border-radius: 8px;">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 6px;">
                        <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
                    </svg>
                    ${group.unitName}
                </h3>`;
            group.resources.forEach(r => {
                const sizeStr = r.fileSize > 1048576 ? (r.fileSize / 1048576).toFixed(1) + 'MB' : Math.round(r.fileSize / 1024) + 'KB';
                html += `<a href="/api/course-resources/download/${r.id}" style="display: flex; align-items: center; padding: 14px 18px; background: white; border-radius: 10px; box-shadow: 0 1px 6px rgba(0,0,0,0.08); margin-bottom: 8px; text-decoration: none; color: #333; gap: 12px; transition: box-shadow 0.2s;" onmouseover="this.style.boxShadow='0 3px 12px rgba(0,0,0,0.15)'" onmouseout="this.style.boxShadow='0 1px 6px rgba(0,0,0,0.08)'">
                    <div style="width: 40px; height: 40px; background: #e3f2fd; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#1565c0" stroke-width="2">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                            <polyline points="7 10 12 15 17 10"/>
                            <line x1="12" y1="15" x2="12" y2="3"/>
                        </svg>
                    </div>
                    <div style="flex: 1; min-width: 0;">
                        <div style="font-weight: 600; font-size: 14px; margin-bottom: 3px;">${r.title}</div>
                        <div style="font-size: 12px; color: #999;">${r.originalFileName} · ${sizeStr} · 다운로드 ${r.downloadCount}회</div>
                    </div>
                </a>`;
            });
            html += `</div>`;
        });
        container.innerHTML = html;
    } catch (e) {
        console.error('Resources load error:', e);
        document.getElementById('resourcesContainer').innerHTML = `<p class="course-empty-message">자료를 불러오지 못했습니다.</p>`;
    }
}

// ============ Q&A ============

async function loadQna() {
    try {
        const res = await fetch(`/api/course-qna/${courseId}`);
        const data = await res.json();
        const listEl = document.getElementById('qnaList');

        if (!data.success || !data.data || data.data.length === 0) {
            listEl.innerHTML = `<p class="course-empty-message" style="padding: 30px;">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
                아직 등록된 질문이 없습니다. 첫 번째 질문을 남겨보세요!</p>`;
            return;
        }

        let html = '';
        data.data.forEach(q => {
            const dateStr = q.createdAt ? q.createdAt.substring(0, 10) : '';
            const badge = q.answered
                ? '<span style="padding: 2px 8px; background: #e8f5e9; color: #2e7d32; border-radius: 10px; font-size: 11px; font-weight: 600;">답변완료</span>'
                : '<span style="padding: 2px 8px; background: #fff3e0; color: #e65100; border-radius: 10px; font-size: 11px; font-weight: 600;">대기중</span>';
            html += `<div style="background: white; border-radius: 10px; box-shadow: 0 1px 6px rgba(0,0,0,0.08); margin-bottom: 10px; overflow: hidden;">
                <div onclick="toggleQnaDetail(this, ${q.id})" style="display: flex; align-items: center; padding: 15px 18px; cursor: pointer; gap: 12px; transition: background 0.2s;" onmouseover="this.style.background='#fafafa'" onmouseout="this.style.background='white'">
                    <div style="flex: 1; min-width: 0;">
                        <div style="font-weight: 600; font-size: 14px; margin-bottom: 4px;">${q.title}</div>
                        <div style="font-size: 12px; color: #999;">${q.userName} · ${dateStr}</div>
                    </div>
                    ${badge}
                    <svg class="qna-toggle-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2" style="flex-shrink: 0; transition: transform 0.2s;">
                        <polyline points="6 9 12 15 18 9"/>
                    </svg>
                </div>
                <div class="qna-detail-panel" style="display: none; padding: 0 18px 18px 18px; border-top: 1px solid #f0f0f0;"></div>
            </div>`;
        });
        listEl.innerHTML = html;
    } catch (e) {
        console.error('QnA load error:', e);
        document.getElementById('qnaList').innerHTML = `<p class="course-empty-message">Q&A를 불러오지 못했습니다.</p>`;
    }
}

async function toggleQnaDetail(headerEl, questionId) {
    const panel = headerEl.nextElementSibling;
    const icon = headerEl.querySelector('.qna-toggle-icon');

    if (panel.style.display === 'block') {
        panel.style.display = 'none';
        icon.style.transform = 'rotate(0deg)';
        return;
    }

    panel.style.display = 'block';
    icon.style.transform = 'rotate(180deg)';
    panel.innerHTML = '<p style="color: #999; font-size: 13px; padding: 10px 0;">불러오는 중...</p>';

    try {
        const res = await fetch(`/api/course-qna/question/${questionId}`);
        const data = await res.json();
        if (!data.success || !data.data) {
            panel.innerHTML = '<p style="color: #f44336;">상세 내용을 불러올 수 없습니다.</p>';
            return;
        }
        const q = data.data;
        let html = `<div style="padding-top: 15px;">
            <div style="font-size: 14px; line-height: 1.7; color: #333; white-space: pre-wrap; margin-bottom: 12px;">${q.content}</div>`;
        if (q.imageUrl) {
            html += `<img src="${q.imageUrl}" style="max-width: 100%; border-radius: 8px; margin-bottom: 12px;" alt="첨부 이미지">`;
        }

        if (q.answers && q.answers.length > 0) {
            html += `<div style="margin-top: 15px;">`;
            q.answers.forEach(a => {
                const roleBadge = a.userRole === 'ADMIN'
                    ? '<span style="padding: 1px 6px; background: #e3f2fd; color: #1565c0; border-radius: 8px; font-size: 11px; margin-left: 5px;">관리자</span>'
                    : '';
                html += `<div style="background: #f8f9fa; border-radius: 8px; padding: 14px; margin-bottom: 8px;">
                    <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                        <span style="font-weight: 600; font-size: 13px;">${a.userName}${roleBadge}</span>
                        <span style="font-size: 12px; color: #999;">${a.createdAt ? a.createdAt.substring(0, 10) : ''}</span>
                    </div>
                    <div style="font-size: 13px; line-height: 1.6; color: #333; white-space: pre-wrap;">${a.content}</div>`;
                if (a.imageUrl) {
                    html += `<img src="${a.imageUrl}" style="max-width: 100%; border-radius: 8px; margin-top: 8px;" alt="첨부 이미지">`;
                }
                html += `</div>`;
            });
            html += `</div>`;
        } else {
            html += `<p style="color: #999; font-size: 13px; text-align: center; padding: 10px;">아직 답변이 없습니다.</p>`;
        }
        html += `</div>`;
        panel.innerHTML = html;
    } catch (e) {
        panel.innerHTML = '<p style="color: #f44336;">오류가 발생했습니다.</p>';
    }
}

let qnaSelectedImageUrl = null;

function onQnaImageSelect(input) {
    const nameEl = document.getElementById('qnaImageName');
    if (input.files && input.files[0]) {
        nameEl.textContent = input.files[0].name;
    } else {
        nameEl.textContent = '';
    }
}

async function submitQuestion() {
    const title = document.getElementById('qnaTitle').value.trim();
    const content = document.getElementById('qnaContent').value.trim();
    if (!title || !content) { alert('제목과 내용을 입력해주세요.'); return; }

    const btn = document.getElementById('qnaSubmitBtn');
    btn.disabled = true;
    btn.textContent = '등록 중...';

    try {
        let imageUrl = null;
        const imageFile = document.getElementById('qnaImage').files[0];
        if (imageFile) {
            const formData = new FormData();
            formData.append('file', imageFile);
            const imgRes = await fetch('/api/course-qna/upload-image', { method: 'POST', body: formData });
            const imgData = await imgRes.json();
            if (imgData.success) imageUrl = imgData.data.imageUrl;
        }

        const res = await fetch(`/api/course-qna/${courseId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, content, imageUrl })
        });
        const result = await res.json();
        if (result.success) {
            document.getElementById('qnaTitle').value = '';
            document.getElementById('qnaContent').value = '';
            document.getElementById('qnaImage').value = '';
            document.getElementById('qnaImageName').textContent = '';
            loadQna();
            alert('질문이 등록되었습니다.');
        } else {
            alert(result.message || '질문 등록에 실패했습니다.');
        }
    } catch (e) {
        alert('오류가 발생했습니다.');
    } finally {
        btn.disabled = false;
        btn.textContent = '질문 등록';
    }
}

// ============ 재수강 ============

function retakeCourse(cId) {
    const csrf = document.querySelector('meta[name="_csrf"]')?.content || '';
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/courses/' + cId + '/retake';
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = '_csrf';
    input.value = csrf;
    form.appendChild(input);
    document.body.appendChild(form);
    form.submit();
}

// ============ 레슨 선택 ============

function selectLesson(cId, lessonId) {
    const isAccessible = document.getElementById('isAccessible')?.value === 'true';
    if (!isAccessible) {
        alert('수강 신청 후 학습할 수 있습니다.');
        return;
    }
    window.location.href = '/lesson?courseId=' + cId + '&lessonId=' + lessonId;
}

// ============ 진도율 업데이트 (향후 사용) ============

function updateProgress() {
    const totalLessons = document.querySelectorAll('.course-lesson-item').length;
    const completedLessons = document.querySelectorAll('.course-lesson-item.is-done').length;
    const progressPercent = Math.round((completedLessons / totalLessons) * 100);

    const progressFill = document.querySelector('.course-progress-fill');
    const progressPercentText = document.querySelector('.course-progress-percent');
    const progressText = document.querySelector('.course-progress-text');

    if (progressFill) progressFill.style.width = `${progressPercent}%`;
    if (progressPercentText) progressPercentText.textContent = `${progressPercent}%`;
    if (progressText) progressText.textContent = `${totalLessons}개 레슨 중 ${completedLessons}개 완료`;
}