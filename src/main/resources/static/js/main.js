// 메인 페이지 JavaScript
// Spring Security가 인증을 처리하므로 sessionStorage 체크 제거

// 학습 페이지에서 subnav 표시 + 현재 탭 active 처리
document.addEventListener("DOMContentLoaded", () => {
    const header = document.getElementById("mainHeader");
    if (!header) return;

    const path = location.pathname;

    const learnPages = [
        { key: "online-learning", path: "/online-learning" },
        { key: "attendance", path: "/attendance" },
        { key: "my-classroom", path: "/my-classroom" }
    ];

    const found = learnPages.find(p => path.startsWith(p.path));
    if (found) {
        header.classList.add("learn-mode");
        document.querySelector(`.subnav-item[data-subnav="${found.key}"]`)
            ?.classList.add("active");
    }
});

// ESC 키로 뒤로가기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        history.back();
    }
});
