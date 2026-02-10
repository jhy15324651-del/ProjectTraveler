document.addEventListener("DOMContentLoaded", () => {
    const btn = document.querySelector("[data-delete-url]");
    if (!btn) return;

    btn.addEventListener("click", async () => {
        const url = btn.getAttribute("data-delete-url");
        if (!url) return;

        const ok = confirm("정말 삭제할까요?");
        if (!ok) return;

        try {
            const res = await fetch(url, {
                method: "POST",
                headers: {
                    ...getCsrfHeaders(),
                    "X-Requested-With": "XMLHttpRequest"
                }
            });

            if (!res.ok) {
                // 서버에서 403/500 등일 때
                const text = await res.text().catch(() => "");
                console.error("delete failed:", res.status, text);
                alert("삭제에 실패했습니다. 권한 또는 서버 상태를 확인해 주세요.");
                return;
            }

            // ✅ 삭제 성공: 목록으로 이동 (서버 redirect가 있어도 fetch는 따라가지 않으니 여기서 이동)
            // web/unity 분기: 현재 경로에 '/reviews-unity/'가 있으면 unity 목록으로
            const isUnity = location.pathname.startsWith("/reviews-unity/");
            location.href = isUnity ? "/reviews-unity" : "/reviews";
        } catch (e) {
            console.error(e);
            alert("삭제 중 오류가 발생했습니다.");
        }
    });

    function getCsrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
        if (!token || !header) return {};
        return { [header]: token };
    }
});
