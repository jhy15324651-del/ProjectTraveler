// 메인 페이지 JavaScript

// 페이지 로드 시 사용자 이름 표시
window.addEventListener('DOMContentLoaded', function() {
    // 로그인 여부 확인
    const isLoggedIn = sessionStorage.getItem('isLoggedIn');
    if (!isLoggedIn) {
        window.location.href = '/index';
        return;
    }
    
    // 사용자 이름 표시
    const username = sessionStorage.getItem('username');
    if (username) {
        const userNameElement = document.getElementById('userName');
        if (userNameElement) {
            userNameElement.textContent = username + '님';
        }
    }
});

// 로그아웃
function logout() {
    if (confirm('로그아웃 하시겠습니까?')) {
        sessionStorage.clear();
        window.location.href = '/index';
    }
}

// ESC 키로 뒤로가기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        window.location.href = '/select';
    }
});

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
        header.classList.add("learn-mode"); // ✅ subnav 보이게
        document.querySelector(`.subnav-item[data-subnav="${found.key}"]`)
            ?.classList.add("active");
    }
});
