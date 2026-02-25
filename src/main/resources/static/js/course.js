// static/js/course.js
// - course-detail.html의 인라인 JS를 외부 파일로 분리한 버전
// ✅ Thymeleaf inline(/*[[...]]*/) 제거
// ✅ courseId / isAccessible / isUnity는 DOM에서 읽음
// ✅ HTML의 onclick="..." 호출을 유지하기 위해 window.* 로 전역 노출
// ✅ (규칙 적용) course-detail / learning / lesson / quiz 4개 이동만 isUnity 분기

(() => {
    "use strict";

    // ---------------------------
    // DOM에서 런타임 값 읽기
    // ---------------------------
    function getCourseId() {
        const el = document.getElementById("courseId");
        const v = el ? String(el.value || "").trim() : "";
        const n = Number(v);
        return Number.isFinite(n) && n > 0 ? n : 0;
    }

    function getIsAccessible() {
        const bodyVal = document.body ? document.body.getAttribute("data-accessible") : null;
        if (bodyVal === "true") return true;
        if (bodyVal === "false") return false;

        const el = document.getElementById("isAccessible");
        if (el) {
            const v = String(el.value || "").trim().toLowerCase();
            if (v === "true") return true;
            if (v === "false") return false;
        }
        return false;
    }

    function getIsUnity() {
        // ✅ 권장: <input type="hidden" id="isUnity" value="true|false">
        const el = document.getElementById("isUnity");
        if (el) {
            const v = String(el.value || "").trim().toLowerCase();
            if (v === "true") return true;
            if (v === "false") return false;
        }

        // 보조: <body data-is-unity="true|false">
        const bodyVal = document.body ? document.body.getAttribute("data-is-unity") : null;
        if (bodyVal === "true") return true;
        if (bodyVal === "false") return false;

        return false;
    }

    // ---------------------------
    // 라우팅 헬퍼 (4개 페이지만 분기)
    // ---------------------------
    const route = {
        learning: (isUnity) => (isUnity ? "/learning-unity" : "/learning"),
        courseDetail: (isUnity) => (isUnity ? "/course-detail-unity" : "/course-detail"),
        lesson: (isUnity) => (isUnity ? "/lesson-unity" : "/lesson"),
        quiz: (isUnity) => (isUnity ? "/quiz-unity" : "/quiz"),
    };

    // ---------------------------
    // 상태 캐시
    // ---------------------------
    const state = {
        courseId: 0,
        isAccessible: false,
        isUnity: false,
    };

    // ---------------------------
    // 수강 신청
    // ---------------------------
    async function requestEnrollment() {
        const courseId = state.courseId;
        if (!courseId) {
            alert("강좌 정보를 불러오지 못했습니다.");
            return;
        }

        const btn = document.getElementById("enrollBtn");
        if (btn) {
            btn.disabled = true;
            btn.textContent = "처리 중...";
        }

        try {
            const response = await fetch("/api/enrollments/request", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ courseId }),
            });

            const result = await response.json();

            if (result.success) {
                alert(result.message);
                location.reload();
            } else {
                alert(result.message);
                if (btn) {
                    btn.disabled = false;
                    btn.textContent = "수강 신청";
                }
            }
        } catch (error) {
            alert("오류가 발생했습니다.");
            if (btn) {
                btn.disabled = false;
                btn.textContent = "수강 신청";
            }
        }
    }

    // ---------------------------
    // 퀴즈별 상태 로드 (여러 퀴즈 카드 각각에 대해)
    // ---------------------------
    async function loadQuizStatuses() {
        const courseId = state.courseId;
        if (!courseId) return;

        const quizCards = document.querySelectorAll(".quiz-status-card");
        if (quizCards.length === 0) {
            loadQuizStatusLegacy();
            return;
        }

        const quizBase = route.quiz(state.isUnity); // ✅ /quiz or /quiz-unity

        for (const card of quizCards) {
            const quizId = card.getAttribute("data-quiz-id");
            if (!quizId) continue;

            try {
                const res = await fetch(`/api/quiz/${quizId}/can-attempt`);
                const data = await res.json();

                // ✅ (원본 유지) 퀴즈별 상세 상태도 가져오기 (누락 방지용)
                const statusRes = await fetch(`/api/quiz/status/${courseId}`);
                await statusRes.json();

                const actionArea = card.querySelector(".quiz-action-area");
                const iconEl = card.querySelector('div[style*="border-radius: 50%"]');

                const histRes = await fetch(`/api/quiz/${quizId}/history`);
                const histData = await histRes.json();

                const passedAttempt =
                    histData.success && histData.data ? histData.data.find((a) => a.passed) : null;

                if (!actionArea || !iconEl) continue;

                if (passedAttempt) {
                    iconEl.textContent = "\u2713";
                    iconEl.style.background = "#e8f5e9";
                    iconEl.style.color = "#2e7d32";
                    const msgEl = card.querySelector('div[style*="font-size: 13px"]');
                    if (msgEl) msgEl.textContent = "최고 점수: " + passedAttempt.scorePercent + "점";
                    actionArea.innerHTML =
                        '<span style="padding: 8px 16px; background: #e8f5e9; color: #2e7d32; border-radius: 20px; font-size: 13px; font-weight: 600;">합격</span>';
                } else if (data.success && data.data && data.data.canAttempt) {
                    // ✅ quiz 이동 분기 적용
                    actionArea.innerHTML = `<a href="${quizBase}?courseId=${courseId}&quizId=${quizId}" style="padding: 10px 24px; background: var(--primary-red); color: white; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px;">퀴즈 응시</a>`;
                } else {
                    const attempts = histData.success && histData.data ? histData.data.length : 0;

                    if (attempts >= 2) {
                        iconEl.textContent = "\uD83D\uDCDA";
                        iconEl.style.background = "#fff3e0";
                        const msgEl = card.querySelector('div[style*="font-size: 13px"]');
                        if (msgEl) msgEl.textContent = "재수강이 필요합니다.";
                        // ✅ quiz 이동 분기 적용 (원본은 "재수강" 버튼이 quiz로 가는 구조라 유지)
                        actionArea.innerHTML = `<a href="${quizBase}?courseId=${courseId}&quizId=${quizId}" style="padding: 10px 24px; background: #ff9800; color: white; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px;">재수강</a>`;
                    } else if (attempts === 1) {
                        iconEl.style.background = "#fff3e0";
                        const msgEl = card.querySelector('div[style*="font-size: 13px"]');
                        if (msgEl) msgEl.textContent = "오답 확인 후 2차 시험에 응시하세요.";
                        // ✅ quiz 이동 분기 적용
                        actionArea.innerHTML = `<a href="${quizBase}?courseId=${courseId}&quizId=${quizId}" style="padding: 10px 24px; background: var(--primary-red); color: white; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px;">퀴즈 응시</a>`;
                    } else {
                        actionArea.innerHTML =
                            '<span style="padding: 8px 16px; background: #f5f5f5; color: #666; border-radius: 20px; font-size: 13px;">응시 불가</span>';
                    }
                }
            } catch (e) {
                console.error("Quiz status load error for quizId=" + quizId, e);
            }
        }
    }

    // ---------------------------
    // 기존 단일 퀴즈 카드 fallback
    // ---------------------------
    async function loadQuizStatusLegacy() {
        const courseId = state.courseId;
        if (!courseId) return;

        try {
            const res = await fetch(`/api/quiz/status/${courseId}`);
            const data = await res.json();
            if (!data.success || !data.data) return;

            const status = data.data;
            const card = document.getElementById("quizStatusCard");
            const icon = document.getElementById("quizStatusIcon");
            const title = document.getElementById("quizStatusTitle");
            const msg = document.getElementById("quizStatusMessage");
            const action = document.getElementById("quizStatusAction");

            if (!card || !icon || !title || !msg || !action) return;

            card.style.display = "block";

            const quizBase = route.quiz(state.isUnity); // ✅ /quiz or /quiz-unity

            if (status.quizStatusCode === "PASSED") {
                icon.textContent = "\u2713";
                icon.style.background = "#e8f5e9";
                icon.style.color = "#2e7d32";
                title.textContent = "퀴즈 합격 완료";
                msg.textContent = "최고 점수: " + status.bestScore + "점";
                action.innerHTML =
                    '<span style="padding: 8px 16px; background: #e8f5e9; color: #2e7d32; border-radius: 20px; font-size: 13px; font-weight: 600;">합격</span>';
            } else if (status.canAttempt) {
                icon.textContent = "\uD83D\uDCDD";
                title.textContent = status.title || "퀴즈";
                msg.textContent = status.message || "퀴즈에 응시할 수 있습니다.";
                // ✅ quiz 이동 분기 적용
                action.innerHTML = `<a href="${quizBase}?courseId=${courseId}&quizId=${status.quizId}" style="padding: 10px 24px; background: var(--primary-red); color: white; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px;">퀴즈 응시</a>`;
            } else if (status.quizStatusCode === "RETAKE_REQUIRED") {
                icon.textContent = "\uD83D\uDCDA";
                icon.style.background = "#fff3e0";
                title.textContent = "재수강 필요";
                msg.textContent = status.message || "강의를 다시 수강한 후 퀴즈에 응시해주세요.";
                // ✅ quiz 이동 분기 적용 (원본 유지)
                action.innerHTML = `<a href="${quizBase}?courseId=${courseId}&quizId=${status.quizId}" style="padding: 10px 24px; background: #ff9800; color: white; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px;">재수강</a>`;
            } else if (status.quizStatusCode === "RETRY_ALLOWED") {
                icon.textContent = "\uD83D\uDCDD";
                icon.style.background = "#fff3e0";
                title.textContent = "2차 시험 응시 가능";
                msg.textContent = "오답 확인 후 2차 시험에 응시하세요.";
                // ✅ quiz 이동 분기 적용
                action.innerHTML = `<a href="${quizBase}?courseId=${courseId}&quizId=${status.quizId}" style="padding: 10px 24px; background: var(--primary-red); color: white; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px;">퀴즈 응시</a>`;
            } else {
                icon.textContent = "\uD83D\uDCDD";
                title.textContent = status.title || "퀴즈";
                msg.textContent = status.message || "";
            }
        } catch (e) {
            console.error("Quiz status load error:", e);
        }
    }

    // ---------------------------
    // 자료실 로드
    // ---------------------------
    async function loadResources() {
        const courseId = state.courseId;
        if (!courseId) return;

        try {
            const res = await fetch(`/api/course-resources/${courseId}`);
            const data = await res.json();
            const container = document.getElementById("resourcesContainer");
            if (!container) return;

            if (!data.success || !data.data || data.data.length === 0) {
                container.innerHTML = `<p class="course-empty-message">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
            </svg>
            등록된 학습 자료가 없습니다.</p>`;
                return;
            }

            let html = "";
            data.data.forEach((group) => {
                html += `<div style="margin-bottom: 20px;">
            <h3 style="font-size: 16px; font-weight: 600; margin: 0 0 12px 0; padding: 10px 15px; background: #f8f9fa; border-radius: 8px;">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 6px;">
                    <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
                </svg>
                ${group.unitName}
            </h3>`;
                group.resources.forEach((r) => {
                    const sizeStr =
                        r.fileSize > 1048576
                            ? (r.fileSize / 1048576).toFixed(1) + "MB"
                            : Math.round(r.fileSize / 1024) + "KB";
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
            console.error("Resources load error:", e);
            const container = document.getElementById("resourcesContainer");
            if (container) {
                container.innerHTML = `<p class="course-empty-message">자료를 불러오지 못했습니다.</p>`;
            }
        }
    }

    // ---------------------------
    // Q&A 로드
    // ---------------------------
    async function loadQna() {
        const courseId = state.courseId;
        if (!courseId) return;

        try {
            const res = await fetch(`/api/course-qna/${courseId}`);
            const data = await res.json();
            const listEl = document.getElementById("qnaList");
            if (!listEl) return;

            if (!data.success || !data.data || data.data.length === 0) {
                listEl.innerHTML = `<p class="course-empty-message" style="padding: 30px;">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            아직 등록된 질문이 없습니다. 첫 번째 질문을 남겨보세요!</p>`;
                return;
            }

            let html = "";
            data.data.forEach((q) => {
                const dateStr = q.createdAt ? q.createdAt.substring(0, 10) : "";
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
            console.error("QnA load error:", e);
            const listEl = document.getElementById("qnaList");
            if (listEl) {
                listEl.innerHTML = `<p class="course-empty-message">Q&A를 불러오지 못했습니다.</p>`;
            }
        }
    }

    // ---------------------------
    // Q&A 상세 토글 (아코디언)
    // ---------------------------
    async function toggleQnaDetail(headerEl, questionId) {
        const panel = headerEl ? headerEl.nextElementSibling : null;
        const icon = headerEl ? headerEl.querySelector(".qna-toggle-icon") : null;
        if (!panel || !icon) return;

        if (panel.style.display === "block") {
            panel.style.display = "none";
            icon.style.transform = "rotate(0deg)";
            return;
        }

        panel.style.display = "block";
        icon.style.transform = "rotate(180deg)";
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
                q.answers.forEach((a) => {
                    const roleBadge =
                        a.userRole === "ADMIN"
                            ? '<span style="padding: 1px 6px; background: #e3f2fd; color: #1565c0; border-radius: 8px; font-size: 11px; margin-left: 5px;">관리자</span>'
                            : "";
                    html += `<div style="background: #f8f9fa; border-radius: 8px; padding: 14px; margin-bottom: 8px;">
              <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                  <span style="font-weight: 600; font-size: 13px;">${a.userName}${roleBadge}</span>
                  <span style="font-size: 12px; color: #999;">${a.createdAt ? a.createdAt.substring(0, 10) : ""}</span>
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

    // ---------------------------
    // 이미지 선택
    // ---------------------------
    let qnaSelectedImageUrl = null; // (원본 유지)
    function onQnaImageSelect(input) {
        const nameEl = document.getElementById("qnaImageName");
        if (!nameEl) return;

        if (input && input.files && input.files[0]) {
            nameEl.textContent = input.files[0].name;
        } else {
            nameEl.textContent = "";
        }
    }

    // ---------------------------
    // 질문 등록
    // ---------------------------
    async function submitQuestion() {
        const courseId = state.courseId;
        if (!courseId) {
            alert("강좌 정보를 불러오지 못했습니다.");
            return;
        }

        const titleEl = document.getElementById("qnaTitle");
        const contentEl = document.getElementById("qnaContent");
        if (!titleEl || !contentEl) return;

        const title = titleEl.value.trim();
        const content = contentEl.value.trim();
        if (!title || !content) {
            alert("제목과 내용을 입력해주세요.");
            return;
        }

        const btn = document.getElementById("qnaSubmitBtn");
        if (btn) {
            btn.disabled = true;
            btn.textContent = "등록 중...";
        }

        try {
            let imageUrl = null;
            const imageInput = document.getElementById("qnaImage");
            const imageFile = imageInput && imageInput.files ? imageInput.files[0] : null;

            if (imageFile) {
                const formData = new FormData();
                formData.append("file", imageFile);

                const imgRes = await fetch("/api/course-qna/upload-image", { method: "POST", body: formData });
                const imgData = await imgRes.json();
                if (imgData.success) imageUrl = imgData.data.imageUrl;
            }

            const res = await fetch(`/api/course-qna/${courseId}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ title, content, imageUrl }),
            });

            const result = await res.json();
            if (result.success) {
                titleEl.value = "";
                contentEl.value = "";
                if (imageInput) imageInput.value = "";
                const nameEl = document.getElementById("qnaImageName");
                if (nameEl) nameEl.textContent = "";
                await loadQna();
                alert("질문이 등록되었습니다.");
            } else {
                alert(result.message || "질문 등록에 실패했습니다.");
            }
        } catch (e) {
            alert("오류가 발생했습니다.");
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.textContent = "질문 등록";
            }
        }
    }

    // ---------------------------
    // 레슨 선택 (영상 재생 페이지로 이동)
    // ---------------------------
    function selectLesson(courseId, lessonId) {
        if (!state.isAccessible) {
            alert("수강 신청 후 학습할 수 있습니다.");
            return;
        }

        // ✅ lesson 이동 분기 적용
        const base = route.lesson(state.isUnity); // /lesson or /lesson-unity
        window.location.href = `${base}?courseId=${courseId}&lessonId=${lessonId}`;
    }

    // ---------------------------
    // 초기화(페이지 로드 시)
    // ---------------------------
    function init() {
        state.courseId = getCourseId();
        state.isAccessible = getIsAccessible();
        state.isUnity = getIsUnity();

        // 수강 가능한 경우 퀴즈/자료/Q&A 로드
        if (state.isAccessible) {
            loadQuizStatuses();
            loadResources();
            loadQna();
        } else {
            const resourcesLoading = document.getElementById("resourcesLoading");
            if (resourcesLoading) {
                resourcesLoading.innerHTML = `
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
          </svg>
          수강 신청 후 자료를 확인할 수 있습니다.`;
            }

            const qnaWriteForm = document.getElementById("qnaWriteForm");
            if (qnaWriteForm) qnaWriteForm.style.display = "none";

            const qnaLoading = document.getElementById("qnaLoading");
            if (qnaLoading) {
                qnaLoading.innerHTML = `
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          수강 신청 후 Q&A를 이용할 수 있습니다.`;
            }
        }
    }

    // ---------------------------
    // 전역 노출 (onclick 유지)
    // ---------------------------
    window.requestEnrollment = requestEnrollment;
    window.loadQuizStatuses = loadQuizStatuses;
    window.loadQuizStatusLegacy = loadQuizStatusLegacy;
    window.loadResources = loadResources;
    window.loadQna = loadQna;
    window.toggleQnaDetail = toggleQnaDetail;
    window.onQnaImageSelect = onQnaImageSelect;
    window.submitQuestion = submitQuestion;
    window.selectLesson = selectLesson;

    document.addEventListener("DOMContentLoaded", init);
})();