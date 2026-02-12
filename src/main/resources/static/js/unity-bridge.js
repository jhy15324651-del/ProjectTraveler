// unity-bridge.js
function sendUserInfoToUnity(name) {
    // 1. 현재 창 혹은 부모 창에서 unityInstance를 찾습니다.
    const target = window.unityInstance || (window.parent && window.parent.unityInstance);

    if (target) {
        console.log("유니티 인스턴스 발견! FullName 전송: " + name);
        //의 SetUnityUsername 함수 호출
        target.SendMessage('ChatManager', 'SetUnityUsername', name);
    } else {
        // 2. 아직 유니티가 로딩 중일 수 있으므로 0.5초 후 다시 시도합니다.
        console.log("유니티 인스턴스를 찾는 중...");
        setTimeout(() => sendUserInfoToUnity(name), 500);
    }
}