/* quiz.js
 * - quiz.html 인라인 스크립트 분리 버전
 * - 기능 변경 없음 (inline onclick 유지)
 * - Thymeleaf 변수는 window.__QUIZ_PAGE__로 주입받음
 * ✅ (규칙 적용) course-detail / learning / lesson / quiz 4개 이동만 isUnity 분기
 */

(() => {
    "use strict";

    // ====== Thymeleaf 주입값 로드 ======
    const cfg = window.__QUIZ_PAGE__ || {};
    const courseId = Number(cfg.courseId || 0);
    const quizIdParam = cfg.quizIdParam ?? null;

    // ✅ isUnity (우선: cfg → 폴백: DOM)
    function getIsUnityFromDom() {
        const el = document.getElementById("isUnity");
        if (el) {
            const v = String(el.value || "").trim().toLowerCase();
            if (v === "true") return true;
            if (v === "false") return false;
        }
        const root = document.body || document.documentElement;
        const bodyVal = root ? root.getAttribute("data-is-unity") : null;
        if (bodyVal === "true") return true;
        if (bodyVal === "false") return false;
        return false;
    }

    const isUnity =
        typeof cfg.isUnity === "boolean"
            ? cfg.isUnity
            : (cfg.isUnity === "true" ? true : (cfg.isUnity === "false" ? false : getIsUnityFromDom()));

    // ✅ 4개 페이지만 분기 라우터
    const route = {
        learning: (isUnity) => (isUnity ? "/learning-unity" : "/learning"),
        courseDetail: (isUnity) => (isUnity ? "/course-detail-unity" : "/course-detail"),
        lesson: (isUnity) => (isUnity ? "/lesson-unity" : "/lesson"),
        quiz: (isUnity) => (isUnity ? "/quiz-unity" : "/quiz"),
    };

    function courseDetailUrl(courseId) {
        return route.courseDetail(isUnity) + "?id=" + courseId;
    }

    // ====== 상태값 ======
    let quizData = null;
    let selectedAnswers = {}; // { questionId: optionId }
    let timerInterval = null;
    let remainingSec = 0;
    let isSubmitting = false; // 중복 제출 방지

    // ============ 초기화 ============
    async function init() {
        try {
            // quizId가 없으면 courseId로 퀴즈 조회
            let quizId = quizIdParam;
            if (!quizId) {
                const courseRes = await fetch(`/api/quiz/course/${courseId}`);
                const courseData = await courseRes.json();
                if (!courseData.success || !courseData.data) {
                    showError('퀴즈 없음', courseData.message || '이 강좌에는 퀴즈가 없습니다.');
                    return;
                }
                quizId = courseData.data.id;
            }

            // 퀴즈 데이터 로드
            const res = await fetch(`/api/quiz/${quizId}`);
            const data = await res.json();

            if (res.status === 403) {
                // 응시 불가 상태 처리
                const statusData = data.data && data.data.quizStatus;
                if (statusData) {
                    handleQuizStatus(statusData);
                } else {
                    showError('응시 불가', data.message || '퀴즈에 응시할 수 없습니다.');
                }
                return;
            }

            if (!data.success || !data.data) {
                showError('오류', data.message || '퀴즈를 불러올 수 없습니다.');
                return;
            }

            quizData = data.data;
            renderQuiz();
        } catch (e) {
            console.error('Quiz load error:', e);
            showError('오류', '퀴즈를 불러오는 중 오류가 발생했습니다.');
        }
    }

    // ============ 상태별 처리 ============
    function handleQuizStatus(status) {
        if (status.quizStatusCode === 'PASSED') {
            showResult({
                passed: true,
                scorePercent: status.bestScore,
                passingScore: status.passingScore,
                message: '이미 합격한 퀴즈입니다.',
                status: 'PASS'
            });
        } else if (status.quizStatusCode === 'RETAKE_REQUIRED') {
            showRetakeRequired(status);
        } else if (status.quizStatusCode === 'RETRY_ALLOWED') {
            // 1차 불합격, 오답확인 후 2차 응시 가능
            showFirstFailResult(status);
        } else {
            showError('응시 불가', status.message || '퀴즈에 응시할 수 없습니다.');
        }
    }

    function showRetakeRequired(status) {
        const loadingView = document.getElementById('loadingView');
        const resultView = document.getElementById('resultView');

        if (loadingView) loadingView.style.display = 'none';
        if (resultView) {
            resultView.style.display = 'block';
            resultView.innerHTML = `
        <div class="result-container">
          <div class="result-icon">📚</div>
          <h2 class="result-title fail">재수강이 필요합니다</h2>
          <p class="result-detail">최고 점수: ${status.bestScore || 0}점 / 합격 기준: ${status.passingScore}점</p>
          <div class="result-message">
            ${status.message || '2차 시험에서도 불합격하여 강의를 다시 수강해야 합니다.'}
          </div>
          <div class="result-actions">
            <button class="result-btn result-btn-primary" onclick="startRetake()">재수강 시작하기</button>
            <a href="${courseDetailUrl(courseId)}" class="result-btn result-btn-secondary">강좌로 돌아가기</a>
          </div>
        </div>`;
        }
    }

    function showFirstFailResult(status) {
        const loadingView = document.getElementById('loadingView');
        const resultView = document.getElementById('resultView');

        if (loadingView) loadingView.style.display = 'none';
        if (resultView) {
            resultView.style.display = 'block';
            resultView.innerHTML = `
        <div class="result-container">
          <div class="result-icon">📝</div>
          <h2 class="result-title fail">1차 시험 불합격</h2>
          <p class="result-detail">점수: ${status.bestScore || 0}점 / 합격 기준: ${status.passingScore}점</p>
          <div class="result-message">
            오답을 확인하거나 바로 2차 시험에 응시할 수 있습니다.
          </div>
          <div class="result-actions">
            <button class="result-btn result-btn-primary" onclick="retakeQuiz()">2차 시험 응시하기</button>
            <button class="result-btn result-btn-secondary" onclick="loadReview(${status.quizId}, 1)">오답 확인하기</button>
          </div>
        </div>`;
        }
    }

    // ============ 퀴즈 렌더링 ============
    function renderQuiz() {
        const loadingView = document.getElementById('loadingView');
        const quizView = document.getElementById('quizView');

        if (loadingView) loadingView.style.display = 'none';
        if (quizView) quizView.style.display = 'block';

        const quizTitle = document.getElementById('quizTitle');
        const quizDesc = document.getElementById('quizDesc');
        const quizQuestionCount = document.getElementById('quizQuestionCount');
        const quizPassingScore = document.getElementById('quizPassingScore');

        if (quizTitle) quizTitle.textContent = quizData.title;
        if (quizDesc) quizDesc.textContent = quizData.description || '';
        if (quizQuestionCount) quizQuestionCount.textContent = quizData.totalQuestions + '문제';
        if (quizPassingScore) quizPassingScore.textContent = '합격 기준: ' + quizData.passingScore + '점';

        if (quizData.timeLimitSec && quizData.timeLimitSec > 0) {
            const timeLimitInfo = document.getElementById('timeLimitInfo');
            const quizTimeLimit = document.getElementById('quizTimeLimit');
            if (timeLimitInfo) timeLimitInfo.style.display = 'flex';
            if (quizTimeLimit) quizTimeLimit.textContent = '제한시간: ' + formatTimerTime(quizData.timeLimitSec);
            startTimer(quizData.timeLimitSec);
        }

        const container = document.getElementById('questionsContainer');
        if (!container) return;

        container.innerHTML = '';

        quizData.questions.forEach((q, idx) => {
            const card = document.createElement('div');
            card.className = 'question-card';
            card.id = 'question-' + q.id;

            let optionsHtml = '';
            q.options.forEach(opt => {
                optionsHtml += `
          <div class="option-item" data-question-id="${q.id}" data-option-id="${opt.id}" onclick="selectOption(${q.id}, ${opt.id})">
            <div class="option-radio"></div>
            <span class="option-text">${escapeHtml(opt.content)}</span>
          </div>`;
            });

            card.innerHTML = `
        <div class="question-number">문제 ${idx + 1}</div>
        <div class="question-text">${escapeHtml(q.question)}</div>
        <div class="option-list">${optionsHtml}</div>`;

            container.appendChild(card);
        });

        updateAnsweredCount();
    }

    // ============ 선택지 선택 ============
    function selectOption(questionId, optionId) {
        selectedAnswers[questionId] = optionId;

        // UI 갱신
        const items = document.querySelectorAll(`.option-item[data-question-id="${questionId}"]`);
        items.forEach(item => {
            if (parseInt(item.dataset.optionId, 10) === optionId) {
                item.classList.add('selected');
            } else {
                item.classList.remove('selected');
            }
        });

        updateAnsweredCount();
    }

    function updateAnsweredCount() {
        const total = quizData ? quizData.questions.length : 0;
        const answered = Object.keys(selectedAnswers).length;

        const countEl = document.getElementById('answeredCount');
        if (countEl) countEl.textContent = answered + ' / ' + total + ' 답변';

        const notice = document.getElementById('unansweredNotice');
        if (!notice) return;

        if (answered < total) {
            notice.style.display = 'block';
            notice.textContent = (total - answered) + '개의 미답변 문제가 있습니다.';
            notice.style.color = ''; // 원래 색으로
        } else {
            notice.style.display = 'none';
        }
    }

    // ============ 타이머 ============
    function stopTimer() {
        if (timerInterval) {
            clearInterval(timerInterval);
            timerInterval = null;
        }
    }

    function startTimer(totalSec) {
        stopTimer(); // 기존 타이머가 있으면 먼저 정리
        remainingSec = totalSec;

        const timerBar = document.getElementById('timerBar');
        if (timerBar) timerBar.style.display = 'flex';

        updateTimerDisplay();

        timerInterval = setInterval(() => {
            remainingSec--;
            updateTimerDisplay();

            if (remainingSec <= 0) {
                stopTimer();
                // alert 대신 비블로킹 방식으로 안내 후 자동 제출
                const notice = document.getElementById('unansweredNotice');
                if (notice) {
                    notice.style.display = 'block';
                    notice.textContent = '시간이 초과되었습니다. 자동으로 제출합니다.';
                    notice.style.color = '#c62828';
                }
                submitQuiz();
            }
        }, 1000);
    }

    function updateTimerDisplay() {
        const el = document.getElementById('timerText');
        const bar = document.getElementById('timerBar');
        if (!el || !bar) return;

        el.textContent = formatTimerTime(remainingSec);

        el.className = 'timer-text';
        bar.className = 'quiz-timer';

        if (remainingSec <= 60) {
            el.classList.add('danger');
            bar.classList.add('danger');
        } else if (remainingSec <= 300) {
            el.classList.add('warning');
            bar.classList.add('warning');
        }
    }

    function formatTimerTime(sec) {
        const m = Math.floor(sec / 60);
        const s = sec % 60;
        return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
    }

    // ============ 제출 ============
    async function submitQuiz() {
        if (isSubmitting) return; // 중복 제출 방지
        isSubmitting = true;
        stopTimer();

        const btn = document.getElementById('submitBtn');
        if (btn) {
            btn.disabled = true;
            btn.textContent = '제출 중...';
        }

        const answers = quizData.questions.map(q => ({
            questionId: q.id,
            selectedOptionId: selectedAnswers[q.id] || null
        }));

        try {
            const res = await fetch('/api/quiz/submit', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ quizId: quizData.id, answers: answers })
            });
            const data = await res.json();

            if (data.success && data.data) {
                showResult(data.data);
            } else {
                showError('제출 실패', data.message || '제출에 실패했습니다.');
            }
        } catch (e) {
            console.error('Submit error:', e);
            showError('제출 오류', '제출 중 오류가 발생했습니다.');
        } finally {
            isSubmitting = false;
        }
    }

    // ============ 결과 표시 ============
    function showResult(result) {
        const loadingView = document.getElementById('loadingView');
        const quizView = document.getElementById('quizView');
        const reviewView = document.getElementById('reviewView');
        const resultView = document.getElementById('resultView');

        if (loadingView) loadingView.style.display = 'none';
        if (quizView) quizView.style.display = 'none';
        if (reviewView) reviewView.style.display = 'none';
        if (resultView) resultView.style.display = 'block';

        const passed = result.passed;
        const score = result.scorePercent;
        const passing = result.passingScore || (quizData ? quizData.passingScore : 80);
        const total = result.totalQuestions || (quizData ? quizData.questions.length : 0);
        const correct = result.correctCount || 0;

        let icon, title, message, actions;

        if (passed) {
            icon = '🎉';
            title = '축하합니다! 합격!';
            message = '퀴즈를 성공적으로 통과했습니다.';
            actions = `<a href="${courseDetailUrl(courseId)}" class="result-btn result-btn-success">강좌로 돌아가기</a>`;
        } else if (result.status === 'RETRY_ALLOWED') {
            icon = '📝';
            title = '1차 시험 불합격';
            message = '오답을 확인하거나 바로 2차 시험에 응시할 수 있습니다.';
            actions = `
        <button class="result-btn result-btn-primary" onclick="retakeQuiz()">2차 시험 응시하기</button>
        <button class="result-btn result-btn-secondary" onclick="loadReview(${quizData.id}, ${result.attemptNo})">오답 확인하기</button>
        <a href="${courseDetailUrl(courseId)}" class="result-btn result-btn-secondary">강좌로 돌아가기</a>`;
        } else if (result.status === 'RETAKE_REQUIRED') {
            icon = '📚';
            title = '2차 시험 불합격';
            message = '강의를 다시 수강한 후 퀴즈에 응시해주세요.';
            actions = `
        <button class="result-btn result-btn-primary" onclick="startRetake()">재수강 시작하기</button>
        <a href="${courseDetailUrl(courseId)}" class="result-btn result-btn-secondary">강좌로 돌아가기</a>`;
        } else {
            icon = '❌';
            title = '불합격';
            message = result.message || '';
            actions = `<a href="${courseDetailUrl(courseId)}" class="result-btn result-btn-secondary">강좌로 돌아가기</a>`;
        }

        if (resultView) {
            resultView.innerHTML = `
        <div class="result-container">
          <div class="result-icon">${icon}</div>
          <h2 class="result-title ${passed ? 'pass' : 'fail'}">${title}</h2>
          <div class="result-score ${passed ? 'pass' : 'fail'}">${score}점</div>
          <p class="result-detail">${total}문제 중 ${correct}문제 정답 (합격 기준: ${passing}점)</p>
          <div class="result-message">${message}</div>
          <div class="result-actions">${actions}</div>
        </div>`;
        }
    }

    // ============ 오답 확인 ============
    async function loadReview(quizId, attemptNo) {
        const resultView = document.getElementById('resultView');
        const reviewView = document.getElementById('reviewView');

        if (resultView) resultView.style.display = 'none';
        if (reviewView) {
            reviewView.style.display = 'block';
            reviewView.innerHTML = '<div class="quiz-loading"><div class="spinner"></div><p>오답을 불러오는 중...</p></div>';
        }

        try {
            const res = await fetch(`/api/quiz/${quizId}/review?attemptNo=${attemptNo}`);
            const data = await res.json();

            if (!data.success || !data.data) {
                if (reviewView) {
                    reviewView.innerHTML = `
            <div class="quiz-error">
              <h2>오답 확인 불가</h2>
              <p>${data.message || '오답 정보를 불러올 수 없습니다.'}</p>
              <a href="${courseDetailUrl(courseId)}">강좌로 돌아가기</a>
            </div>`;
                }
                return;
            }

            renderReview(data.data);
        } catch (e) {
            console.error('Review load error:', e);
            if (reviewView) {
                reviewView.innerHTML = `
          <div class="quiz-error">
            <h2>오류</h2>
            <p>오답 정보를 불러오는 중 오류가 발생했습니다.</p>
            <a href="${courseDetailUrl(courseId)}">강좌로 돌아가기</a>
          </div>`;
            }
        }
    }

    function renderReview(review) {
        const reviewView = document.getElementById('reviewView');
        if (!reviewView) return;

        let html = `
      <div class="quiz-header">
        <h1 class="quiz-title">오답 확인</h1>
        <p class="quiz-desc">점수: ${review.scorePercent}점 | ${review.attemptNo}차 시험</p>
      </div>`;

        review.questions.forEach((q, idx) => {
            const isCorrect = q.isCorrect;
            const badgeClass = isCorrect ? 'correct' : 'wrong';
            const badgeText = isCorrect ? '정답' : '오답';

            let optionsHtml = '';
            q.options.forEach(opt => {
                let cls = '';
                if (opt.id === q.correctOptionId) cls = 'correct';
                else if (opt.id === q.selectedOptionId && !isCorrect) cls = 'wrong';

                optionsHtml += `
          <div class="option-item ${cls}" style="cursor: default;">
            <div class="option-radio"></div>
            <span class="option-text">${escapeHtml(opt.content)}</span>
            ${opt.id === q.correctOptionId ? '<span style="margin-left: auto; color: #2e7d32; font-size: 13px; font-weight: 600;">정답</span>' : ''}
            ${opt.id === q.selectedOptionId && opt.id !== q.correctOptionId ? '<span style="margin-left: auto; color: #c62828; font-size: 13px; font-weight: 600;">내 선택</span>' : ''}
          </div>`;
            });

            html += `
        <div class="question-card">
          <div class="question-number">
            문제 ${idx + 1}
            <span class="question-result-badge ${badgeClass}">${badgeText}</span>
          </div>
          <div class="question-text">${escapeHtml(q.question)}</div>
          <div class="option-list">${optionsHtml}</div>
          ${q.explanation ? `<div style="margin-top: 15px; padding: 12px; background: #f5f5f5; border-radius: 8px; font-size: 14px; color: #555;"><strong>해설:</strong> ${escapeHtml(q.explanation)}</div>` : ''}
        </div>`;
        });

        html += `
      <div class="quiz-submit-area">
        <div class="result-actions">
          <button class="result-btn result-btn-primary" onclick="retakeQuiz()">2차 시험 응시하기</button>
          <a href="${courseDetailUrl(courseId)}" class="result-btn result-btn-secondary">강좌로 돌아가기</a>
        </div>
      </div>`;

        reviewView.innerHTML = html;
    }

    // ============ 2차 시험 응시 ============
    function retakeQuiz() {
        // 전체 상태 초기화
        selectedAnswers = {};
        isSubmitting = false;
        stopTimer();

        const reviewView = document.getElementById('reviewView');
        const resultView = document.getElementById('resultView');
        const loadingView = document.getElementById('loadingView');
        const timerBar = document.getElementById('timerBar');

        if (reviewView) reviewView.style.display = 'none';
        if (resultView) resultView.style.display = 'none';
        if (loadingView) loadingView.style.display = 'block';
        if (timerBar) timerBar.style.display = 'none';

        // 제출 버튼 상태 초기화
        const btn = document.getElementById('submitBtn');
        if (btn) {
            btn.disabled = false;
            btn.textContent = '제출하기';
        }

        init();
    }

    // ============ 재수강 시작 ============
    function startRetake() {
        const csrf = document.querySelector('meta[name="_csrf"]')?.content || '';
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/courses/' + courseId + '/retake';
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = '_csrf';
        input.value = csrf;
        form.appendChild(input);
        document.body.appendChild(form);
        form.submit();
    }

    // ============ 에러/유틸 ============
    function showError(title, message) {
        const loadingView = document.getElementById('loadingView');
        const errorView = document.getElementById('errorView');
        const errorTitle = document.getElementById('errorTitle');
        const errorMessage = document.getElementById('errorMessage');

        if (loadingView) loadingView.style.display = 'none';
        if (errorView) errorView.style.display = 'block';
        if (errorTitle) errorTitle.textContent = title;
        if (errorMessage) errorMessage.textContent = message;
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // ====== HTML inline onclick 유지용: 전역 함수 등록 ======
    window.submitQuiz = submitQuiz;
    window.selectOption = selectOption;
    window.loadReview = loadReview;
    window.retakeQuiz = retakeQuiz;
    window.startRetake = startRetake;

    // ====== 시작 ======
    document.addEventListener('DOMContentLoaded', () => {
        init();
    });

    // (선택) 페이지 이탈 시 타이머 정리 - 기능변화는 없고 안정성만 증가
    window.addEventListener('beforeunload', () => {
        stopTimer();
    });
})();