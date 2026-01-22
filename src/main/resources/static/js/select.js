// 선택 페이지 JavaScript

// 체험하기 클릭 시 (Unity WebGL로 이동)
function goToExperience() {
    // Unity WebGL URL이 있는 경우 해당 URL로 이동
    // 예시: window.location.href = 'https://your-unity-webgl-url.com';
    
    // 현재는 알림 메시지 표시
    alert('Unity WebGL 체험 페이지로 이동합니다.\n\n실제 프로젝트에서는 Unity WebGL 빌드 URL을 연결해주세요.');
    
    // 개발용: Unity WebGL URL을 여기에 입력하세요
    // window.location.href = 'YOUR_UNITY_WEBGL_URL';
}

// 키보드 ESC로 이전 페이지 이동
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        window.location.href = 'index.html';
    }
});
