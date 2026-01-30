/**
 * LMS 학습 진도 관리 JavaScript
 * - 10초마다 heartbeat 전송
 * - 90% 이상 시청 시 완료 처리
 * - 페이지 이탈 시 현재 위치 저장
 */

class LearningProgress {
    constructor(config) {
        this.courseId = config.courseId;
        this.lessonId = config.lessonId;
        this.durationSec = config.durationSec || 0;
        this.videoType = config.videoType || 'NONE';

        this.lastSavedPosition = 0;
        this.totalWatchedSec = 0;
        this.isCompleted = false;
        this.heartbeatInterval = null;
        this.lastHeartbeatTime = 0;

        // 90% 이상 시청 시 완료 처리
        this.completionThreshold = 0.9;

        this.init();
    }

    async init() {
        await this.loadProgress();
        this.setupEventListeners();
    }

    // 초기 진도 로드
    async loadProgress() {
        try {
            const response = await fetch(
                `/api/learning/progress?courseId=${this.courseId}&lessonId=${this.lessonId}`
            );
            const result = await response.json();

            if (result.success && result.data) {
                this.lastSavedPosition = result.data.lastPositionSec || 0;
                this.totalWatchedSec = result.data.watchedSec || 0;
                this.isCompleted = result.data.completed || false;
                this.updateUI();

                if (this.isCompleted) {
                    this.markCompletedUI();
                }
            }
        } catch (e) {
            console.error('Failed to load progress:', e);
        }
    }

    // UI 업데이트
    updateUI() {
        const percent = this.durationSec > 0
            ? Math.min(Math.round((this.totalWatchedSec / this.durationSec) * 100), 100)
            : 0;

        const progressEl = document.getElementById('lessonProgress');
        const barEl = document.getElementById('lessonProgressBar');
        const watchedEl = document.getElementById('watchedTime');

        if (progressEl) progressEl.textContent = percent + '%';
        if (barEl) barEl.style.width = percent + '%';
        if (watchedEl) watchedEl.textContent = this.formatTime(this.totalWatchedSec);
    }

    formatTime(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        if (mins > 0) {
            return mins + '분';
        }
        return secs + '초';
    }

    // 완료 UI 표시
    markCompletedUI() {
        const btn = document.getElementById('completeBtn');
        if (btn) {
            btn.textContent = '완료됨';
            btn.classList.add('completed');
        }
    }

    // Heartbeat 전송
    async sendHeartbeat(positionSec, deltaSec) {
        if (deltaSec <= 0) return;

        try {
            await fetch('/api/learning/heartbeat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    courseId: this.courseId,
                    lessonId: this.lessonId,
                    positionSec: positionSec,
                    deltaWatchedSec: deltaSec
                })
            });

            this.totalWatchedSec += deltaSec;
            this.updateUI();

            console.log(`Heartbeat sent: position=${positionSec}, delta=${deltaSec}`);
        } catch (e) {
            console.error('Heartbeat failed:', e);
        }
    }

    // 레슨 완료 처리
    async markComplete() {
        if (this.isCompleted) return;

        try {
            const response = await fetch('/api/learning/complete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    courseId: this.courseId,
                    lessonId: this.lessonId
                })
            });

            const result = await response.json();

            if (result.success && result.data && result.data.completed) {
                this.isCompleted = true;
                this.markCompletedUI();
                alert('레슨을 완료했습니다!');
                return true;
            } else {
                alert(result.message || '완료 조건을 충족하지 못했습니다. 영상을 더 시청해주세요.');
                return false;
            }
        } catch (e) {
            console.error('Complete failed:', e);
            alert('오류가 발생했습니다.');
            return false;
        }
    }

    // 자동 완료 체크 (90% 이상 시청 시)
    checkAutoComplete() {
        if (this.isCompleted) return;
        if (this.durationSec <= 0) return;

        const progress = this.totalWatchedSec / this.durationSec;
        if (progress >= this.completionThreshold) {
            console.log('Auto-complete triggered: progress=' + (progress * 100).toFixed(1) + '%');
            this.markComplete();
        }
    }

    // HTML5 Video 설정
    setupVideoPlayer() {
        const video = document.getElementById('video-player');
        if (!video) return;

        let lastTime = 0;

        video.addEventListener('loadedmetadata', () => {
            if (this.lastSavedPosition > 0) {
                video.currentTime = this.lastSavedPosition;
            }
        });

        video.addEventListener('timeupdate', () => {
            const currentTime = Math.floor(video.currentTime);
            const delta = currentTime - lastTime;

            // 10초마다 heartbeat
            if (delta >= 10) {
                this.sendHeartbeat(currentTime, delta);
                lastTime = currentTime;
                this.checkAutoComplete();
            }
        });

        video.addEventListener('pause', () => {
            const currentTime = Math.floor(video.currentTime);
            const delta = currentTime - lastTime;
            if (delta > 0) {
                this.sendHeartbeat(currentTime, delta);
                lastTime = currentTime;
            }
        });

        video.addEventListener('ended', () => {
            const delta = this.durationSec - lastTime;
            if (delta > 0) {
                this.sendHeartbeat(this.durationSec, delta);
            }
            this.checkAutoComplete();
        });
    }

    // YouTube 트래킹 (타이머 방식)
    setupYouTubeTracking() {
        if (this.videoType !== 'YOUTUBE') return;

        let watchedSec = this.lastSavedPosition;

        this.heartbeatInterval = setInterval(() => {
            watchedSec += 10;
            this.sendHeartbeat(watchedSec, 10);
            this.checkAutoComplete();
        }, 10000); // 10초마다
    }

    // 이벤트 리스너 설정
    setupEventListeners() {
        if (this.videoType === 'MP4' || this.videoType === 'HLS') {
            this.setupVideoPlayer();
        } else if (this.videoType === 'YOUTUBE') {
            this.setupYouTubeTracking();
        }

        // 페이지 이탈 시 정리
        window.addEventListener('beforeunload', () => {
            if (this.heartbeatInterval) {
                clearInterval(this.heartbeatInterval);
            }
        });
    }

    // 정리
    destroy() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
        }
    }
}

// 전역 함수: 레슨 이동
function goToLesson(courseId, targetLessonId, currentLessonId) {
    if (targetLessonId !== currentLessonId) {
        window.location.href = `/lesson?courseId=${courseId}&lessonId=${targetLessonId}`;
    }
}