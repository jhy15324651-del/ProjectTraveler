// 로그인 폼 처리
document.getElementById('loginForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const remember = document.getElementById('remember').checked;
    
    // 간단한 검증 (실제 프로젝트에서는 서버 인증 필요)
    if (username && password) {
        // 로그인 정보 저장
        if (remember) {
            localStorage.setItem('username', username);
        }
        sessionStorage.setItem('isLoggedIn', 'true');
        sessionStorage.setItem('username', username);
        
        // 선택 페이지로 이동
        window.location.href = 'select.html';
    } else {
        alert('아이디와 비밀번호를 입력해주세요.');
    }
});

// 저장된 아이디가 있으면 자동 입력
window.addEventListener('DOMContentLoaded', function() {
    const savedUsername = localStorage.getItem('username');
    if (savedUsername) {
        document.getElementById('username').value = savedUsername;
        document.getElementById('remember').checked = true;
    }
});
