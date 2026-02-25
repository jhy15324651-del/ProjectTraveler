// /static/js/lesson.js
// ✅ lesson.html의 th:inline="javascript" 인라인 스크립트를 분리한 버전
// ✅ 누락 없이 그대로 동작하도록:
// - Thymeleaf 값은 DOM에서 읽음 (#courseId / data-*)
// - onclick="markComplete()" / goToLesson(...) 유지 가능하도록 window에 함수 노출
// - YouTube IFrame API 콜백(window.onYouTubeIframeAPIReady) 전역 유지
// - DOMContentLoaded 이후 초기화
// ✅ (규칙 적용) course-detail / learning / lesson / quiz 4개 이동만 isUnity 분기

(function () {
    "use strict";

    // -----------------------------
    // 0) Thymeleaf 값 주입 대체 (DOM에서 읽기)
    // -----------------------------
    function readNumberAttr(el, attrName, fallback) {
        if (!el) return fallback;
        const v = el.getAttribute(attrName);
        if (v == null || String(v).trim() === "") return fallback;
        const n = Number(v);
        return Number.isFinite(n) ? n : fallback;
    }

    function readBoolAttr(el, attrName, fallback) {
        if (!el) return fallback;
        const v = (el.getAttribute(attrName) || "").toLowerCase().trim();
        if (v === "true" || v === "1" || v === "yes") return true;
        if (v === "false" || v === "0" || v === "no") return false;
        return fallback;
    }

    function readStringAttr(el, attrName, fallback) {
        if (!el) return fallback;
        const v = el.getAttribute(attrName);
        return (v == null || String(v).trim() === "") ? fallback : v;
    }

    function getCourseIdFromDom() {
        // ✅ course-detail에서 쓰던 방식과 동일하게 hidden input 활용 가능
        const input = document.getElementById("courseId");
        if (input && input.value) {
            const n = Number(input.value);
            if (Number.isFinite(n)) return n;
        }

        // ✅ lesson 페이지에도 hidden이 없다면 아래 data-*를 붙여서 전달 가능
        // 예) <body ... data-course-id="1" data-lesson-id="2" ...>
        const root = document.body || document.documentElement;
        return readNumberAttr(root, "data-course-id", 0);
    }

    function getIsUnityFromDom() {
        // ✅ 권장: <input type="hidden" id="isUnity" value="true|false">
        const el = document.getElementById("isUnity");
        if (el) {
            const v = String(el.value || "").trim().toLowerCase();
            if (v === "true") return true;
            if (v === "false") return false;
        }

        // 보조: <body data-is-unity="true|false">
        const root = document.body || document.documentElement;
        const bodyVal = root ? root.getAttribute("data-is-unity") : null;
        if (bodyVal === "true") return true;
        if (bodyVal === "false") return false;

        return false;
    }

    // ✅ 4개 페이지만 분기 라우터
    const route = {
        learning: (isUnity) => (isUnity ? "/learning-unity" : "/learning"),
        courseDetail: (isUnity) => (isUnity ? "/course-detail-unity" : "/course-detail"),
        lesson: (isUnity) => (isUnity ? "/lesson-unity" : "/lesson"),
        quiz: (isUnity) => (isUnity ? "/quiz-unity" : "/quiz"),
    };

    function getLessonVarsFromDom() {
        const root = document.body || document.documentElement;

        const courseId = getCourseIdFromDom();
        const lessonId = readNumberAttr(root, "data-lesson-id", 0);
        const durationSec = readNumberAttr(root, "data-duration-sec", 0);
        const videoType = readStringAttr(root, "data-video-type", "NONE"); // YOUTUBE / MP4 / HLS / NONE
        const canAccess = readBoolAttr(root, "data-can-access", false);
        const isUnity = getIsUnityFromDom();

        return { courseId, lessonId, durationSec, videoType, canAccess, isUnity };
    }

    // -----------------------------
    // 1) 상태 변수 (인라인과 동일)
    // -----------------------------
    let courseId = 0;
    let lessonId = 0;
    let durationSec = 0;
    let videoType = "NONE";
    let canAccess = false;
    let isUnity = false;

    let lastSavedPosition = 0;
    let totalWatchedSec = 0;
    let heartbeatInterval = null;
    let isCompleted = false;

    // YouTube tracking
    let youtubePlayer = null;
    let isYoutubePlaying = false;
    let youtubeLastTime = 0;

    // -----------------------------
    // 2) 유틸 / UI
    // -----------------------------
    function byId(id) {
        return document.getElementById(id);
    }

    function formatTime(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        if (mins > 0) return mins + "분";
        return secs + "초";
    }

    function updateProgressUI() {
        const percent = durationSec > 0 ? Math.min(Math.round((totalWatchedSec / durationSec) * 100), 100) : 0;

        const elPercent = byId("lessonProgress");
        const elBar = byId("lessonProgressBar");
        const elWatched = byId("watchedTime");

        if (elPercent) elPercent.textContent = percent + "%";
        if (elBar) elBar.style.width = percent + "%";
        if (elWatched) elWatched.textContent = formatTime(totalWatchedSec);
    }

    // -----------------------------
    // 3) API
    // -----------------------------
    async function loadProgress() {
        try {
            const response = await fetch(`/api/learning/progress?courseId=${courseId}&lessonId=${lessonId}`);
            const result = await response.json();
            if (result.success && result.data) {
                lastSavedPosition = result.data.lastPositionSec || 0;
                totalWatchedSec = result.data.watchedSec || 0;
                isCompleted = result.data.completed || false;

                updateProgressUI();

                if (isCompleted) {
                    const btn = byId("completeBtn");
                    if (btn) {
                        btn.textContent = "완료됨 ✓";
                        btn.classList.add("completed");
                    }
                }
            }
        } catch (e) {
            console.error("Failed to load progress:", e);
        }
    }

    async function sendHeartbeat(positionSec, deltaSec) {
        if (!canAccess || deltaSec <= 0) return;

        try {
            await fetch("/api/learning/heartbeat", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    courseId: courseId,
                    lessonId: lessonId,
                    positionSec: positionSec,
                    deltaWatchedSec: deltaSec
                })
            });

            totalWatchedSec += deltaSec;
            updateProgressUI();
        } catch (e) {
            console.error("Heartbeat failed:", e);
        }
    }

    async function markComplete() {
        if (isCompleted) return;

        try {
            const response = await fetch("/api/learning/complete", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ courseId: courseId, lessonId: lessonId })
            });

            const result = await response.json();

            if (result.success && result.data && result.data.completed) {
                isCompleted = true;

                const btn = byId("completeBtn");
                if (btn) {
                    btn.textContent = "완료됨 ✓";
                    btn.classList.add("completed");
                }

                alert("레슨을 완료했습니다!");
            } else {
                alert(result.message || "완료 조건을 충족하지 못했습니다. 영상을 더 시청해주세요.");
            }
        } catch (e) {
            console.error("Complete failed:", e);
            alert("오류가 발생했습니다.");
        }
    }

    async function checkQuizStatus() {
        try {
            const res = await fetch(`/api/quiz/status/${courseId}`);
            const data = await res.json();
            if (!data.success || !data.data) return;

            const status = data.data;
            const notice = byId("quizNotice");
            const text = byId("quizNoticeText");
            const link = byId("quizNoticeLink");

            if (!notice || !text || !link) return;

            const quizBase = route.quiz(isUnity); // ✅ /quiz or /quiz-unity

            if (status.quizStatusCode === "PASSED") {
                notice.style.display = "block";
                notice.style.background = "#e8f5e9";
                text.style.color = "#2e7d32";
                text.textContent = "퀴즈 합격 완료 (" + status.bestScore + "점)";
                link.style.display = "none";
            } else if (status.canAttempt) {
                notice.style.display = "block";
                notice.style.background = "#fff3e0";
                text.style.color = "#e65100";
                text.textContent = "모든 레슨을 완료하셨나요? 퀴즈에 응시해보세요!";
                link.style.display = "block";
                // ✅ quiz 이동 분기 적용
                link.href = `${quizBase}?courseId=${courseId}&quizId=${status.quizId}`;
                link.textContent = "퀴즈 응시하기";
            } else if (status.quizStatusCode === "RETRY_ALLOWED") {
                notice.style.display = "block";
                notice.style.background = "#fff3e0";
                text.style.color = "#e65100";
                text.textContent = "오답 확인 후 2차 시험에 응시할 수 있습니다.";
                link.style.display = "block";
                // ✅ quiz 이동 분기 적용
                link.href = `${quizBase}?courseId=${courseId}&quizId=${status.quizId}`;
                link.textContent = "2차 시험 응시";
            }
        } catch (e) {
            console.error("Quiz status check error:", e);
        }
    }

    // -----------------------------
    // 4) 네비게이션 (인라인과 동일 동작)
    // -----------------------------
    function goToLesson(targetLessonId) {
        const tid = Number(targetLessonId);
        if (!Number.isFinite(tid)) return;
        if (tid !== lessonId) {
            // ✅ lesson 이동 분기 적용
            const lessonBase = route.lesson(isUnity); // /lesson or /lesson-unity
            window.location.href = `${lessonBase}?courseId=${courseId}&lessonId=${tid}`;
        }
    }

    // -----------------------------
    // 5) HTML5 Video Tracking (MP4/HLS)
    // -----------------------------
    function setupVideoPlayer() {
        const video = byId("video-player");
        if (!video) return;

        let lastTime = 0;
        let isPlaying = false;

        video.addEventListener("loadedmetadata", function () {
            if (lastSavedPosition > 0) {
                try {
                    video.currentTime = lastSavedPosition;
                } catch (e) {
                    // ignore
                }
            }
        });

        video.addEventListener("play", function () {
            isPlaying = true;
            lastTime = Math.floor(video.currentTime);
        });

        video.addEventListener("timeupdate", function () {
            if (!isPlaying || video.paused) return;

            const currentTime = Math.floor(video.currentTime);
            const delta = currentTime - lastTime;

            // 10초마다 heartbeat (비정상적인 점프 방지: delta <= 15)
            if (delta >= 10 && delta <= 15) {
                sendHeartbeat(currentTime, delta);
                lastTime = currentTime;
            } else if (delta > 15) {
                // 시크로 인한 점프는 무시하고 위치만 갱신
                lastTime = currentTime;
            }
        });

        video.addEventListener("pause", function () {
            isPlaying = false;
            const currentTime = Math.floor(video.currentTime);
            const delta = currentTime - lastTime;
            if (delta > 0 && delta <= 15) {
                sendHeartbeat(currentTime, delta);
            }
            lastTime = currentTime;
        });

        video.addEventListener("seeking", function () {
            lastTime = Math.floor(video.currentTime);
        });

        video.addEventListener("ended", function () {
            isPlaying = false;
            const currentTime = Math.floor(video.currentTime);
            const delta = currentTime - lastTime;
            if (delta > 0 && delta <= 15) {
                sendHeartbeat(currentTime, delta);
            }
        });
    }

    // -----------------------------
    // 6) YouTube Tracking
    // -----------------------------
    function setupYouTubeTracking() {
        if (videoType !== "YOUTUBE") return;

        const tag = document.createElement("script");
        tag.src = "https://www.youtube.com/iframe_api";
        const firstScriptTag = document.getElementsByTagName("script")[0];
        if (firstScriptTag && firstScriptTag.parentNode) {
            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
        } else {
            document.head.appendChild(tag);
        }
    }

    // ✅ YouTube API가 준비되면 호출됨 (전역)
    window.onYouTubeIframeAPIReady = function () {
        const iframe = byId("youtube-player");
        if (!iframe) return;
        if (typeof YT === "undefined" || !YT.Player) return;

        youtubePlayer = new YT.Player("youtube-player", {
            events: {
                onReady: onYouTubePlayerReady,
                onStateChange: onYouTubePlayerStateChange
            }
        });
    };

    function onYouTubePlayerReady() {
        if (!youtubePlayer) return;
        if (lastSavedPosition > 0) {
            try {
                youtubePlayer.seekTo(lastSavedPosition, true);
            } catch (e) {
                // ignore
            }
        }
    }

    function onYouTubePlayerStateChange(event) {
        if (!youtubePlayer || typeof YT === "undefined" || !YT.PlayerState) return;

        if (event.data === YT.PlayerState.PLAYING) {
            isYoutubePlaying = true;
            youtubeLastTime = Math.floor(youtubePlayer.getCurrentTime());

            if (!heartbeatInterval) {
                heartbeatInterval = setInterval(() => {
                    if (isYoutubePlaying && youtubePlayer) {
                        const currentTime = Math.floor(youtubePlayer.getCurrentTime());
                        const delta = currentTime - youtubeLastTime;

                        if (delta > 0 && delta <= 15) {
                            sendHeartbeat(currentTime, delta);
                        }
                        youtubeLastTime = currentTime;
                    }
                }, 10000);
            }
        } else if (event.data === YT.PlayerState.PAUSED) {
            isYoutubePlaying = false;

            const currentTime = Math.floor(youtubePlayer.getCurrentTime());
            const delta = currentTime - youtubeLastTime;
            if (delta > 0 && delta <= 15) {
                sendHeartbeat(currentTime, delta);
            }
            youtubeLastTime = currentTime;
        } else if (event.data === YT.PlayerState.ENDED) {
            isYoutubePlaying = false;

            const currentTime = Math.floor(youtubePlayer.getCurrentTime());
            const delta = currentTime - youtubeLastTime;
            if (delta > 0) {
                sendHeartbeat(currentTime, delta);
            }

            if (heartbeatInterval) {
                clearInterval(heartbeatInterval);
                heartbeatInterval = null;
            }
        }
    }

    // -----------------------------
    // 7) 초기화
    // -----------------------------
    function init() {
        const vars = getLessonVarsFromDom();
        courseId = vars.courseId;
        lessonId = vars.lessonId;
        durationSec = vars.durationSec;
        videoType = vars.videoType;
        canAccess = vars.canAccess;
        isUnity = vars.isUnity;

        // ✅ 기존 인라인과 동일한 조건
        if (canAccess) {
            loadProgress();
            checkQuizStatus();

            if (videoType === "MP4" || videoType === "HLS") {
                setupVideoPlayer();
            } else if (videoType === "YOUTUBE") {
                setupYouTubeTracking();
            }
        }

        window.addEventListener("beforeunload", function () {
            if (heartbeatInterval) clearInterval(heartbeatInterval);
        });

        // ✅ onclick 핸들러가 HTML에 남아있으므로 전역 노출
        window.markComplete = markComplete;
        window.goToLesson = goToLesson;
    }

    document.addEventListener("DOMContentLoaded", init);
})();