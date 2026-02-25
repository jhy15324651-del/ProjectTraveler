// /static/js/registration.js
// ✅ learning.html 수강 신청 기능 외부 파일 분리
// ✅ 기능 변화 없음
// ✅ inline onclick="requestEnrollment(event, this)" 유지 위해 window에 노출

(() => {
    "use strict";

    // 수강 신청 함수
    async function requestEnrollment(event, button) {
        // 카드 링크 클릭 방지
        if (event) {
            event.preventDefault();
            event.stopPropagation();
        }

        if (!button || !button.dataset) return;

        const courseId = button.dataset.courseId;
        const policy = button.dataset.policy; // (원본 유지: 현재는 사용하지 않지만 누락 방지)

        // 버튼 비활성화
        button.disabled = true;
        const originalText = button.innerHTML;
        button.innerHTML = "처리 중...";

        try {
            const response = await fetch("/api/enrollments/request", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ courseId: parseInt(courseId, 10) }),
            });

            const result = await response.json();

            if (result.success) {
                alert(result.message);
                // 페이지 새로고침하여 상태 업데이트
                location.reload();
            } else {
                alert(result.message || "수강 신청에 실패했습니다.");
                button.disabled = false;
                button.innerHTML = originalText;
            }
        } catch (error) {
            console.error("수강 신청 오류:", error);
            alert("오류가 발생했습니다. 다시 시도해주세요.");
            button.disabled = false;
            button.innerHTML = originalText;
        }
    }

    // inline onclick 유지용
    window.requestEnrollment = requestEnrollment;
})();