// 출석체크 페이지 JavaScript (서버 API 연동)

// 페이지 로드 시 실행
window.addEventListener('DOMContentLoaded', function() {
    // Thymeleaf로 이미 서버 데이터가 렌더링되어 있음
    console.log('Attendance page loaded');
});

// 출석 체크 (서버 API 호출)
async function checkAttendance() {
    const btn = document.getElementById('checkInBtn');
    if (!btn) return;

    btn.disabled = true;
    btn.textContent = '처리 중...';

    try {
        const response = await fetch('/api/attendance/check-in', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (result.success) {
            // 출석 성공
            btn.textContent = '오늘 출석 완료';
            btn.classList.remove('btn-primary');
            btn.classList.add('btn-secondary');

            const messageEl = document.getElementById('checkInMessage');
            if (messageEl) {
                if (result.data && result.data.consecutiveDays) {
                    messageEl.textContent = `축하합니다! 연속 ${result.data.consecutiveDays}일 출석 중입니다!`;
                } else {
                    messageEl.textContent = result.message;
                }
                messageEl.style.color = 'var(--primary-red)';
            }

            // 통계 업데이트
            if (result.data) {
                updateStats(result.data);
            }

            // 2초 후 페이지 새로고침 (캘린더 갱신)
            setTimeout(() => {
                location.reload();
            }, 2000);
        } else {
            // 이미 출석한 경우
            btn.textContent = '오늘 출석 완료';
            btn.classList.remove('btn-primary');
            btn.classList.add('btn-secondary');

            const messageEl = document.getElementById('checkInMessage');
            if (messageEl) {
                messageEl.textContent = result.message || '이미 오늘 출석했습니다.';
            }
        }
    } catch (error) {
        console.error('Check-in error:', error);
        btn.disabled = false;
        btn.textContent = '오늘 출석하기';
        alert('출석 처리 중 오류가 발생했습니다.');
    }
}

// 통계 업데이트
function updateStats(stats) {
    if (stats.totalDays !== undefined) {
        const totalEl = document.getElementById('totalDays');
        if (totalEl) totalEl.textContent = stats.totalDays;
    }

    if (stats.consecutiveDays !== undefined) {
        const consecutiveEl = document.getElementById('consecutiveDays');
        if (consecutiveEl) consecutiveEl.textContent = stats.consecutiveDays;
    }

    if (stats.thisMonthDays !== undefined) {
        const thisMonthEl = document.getElementById('thisMonth');
        if (thisMonthEl) thisMonthEl.textContent = stats.thisMonthDays;
    }
}