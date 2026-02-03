// reviews-post.js (최종 완전본)
document.addEventListener("DOMContentLoaded", () => {

    // =========================
    // 유틸
    // =========================
    const qs = (sel, root = document) => root.querySelector(sel);
    const qsa = (sel, root = document) => [...root.querySelectorAll(sel)];
    const fmt = (n) => Number(n || 0).toLocaleString("ko-KR");
    const onlyNumber = (str) => (str || "").replace(/[^0-9]/g, "");

    const getHidden = (name) => qs(`input[name="${name}"]`);
    const getHiddenValue = (name) => getHidden(name)?.value?.trim() || "";

    const readBudget = (name) => {
        const el = qs(`input[name="${name}"]`);
        if (!el) return 0;
        const v = onlyNumber(el.value);
        return v ? parseInt(v, 10) : 0;
    };

    // ✅ 작성 폼 찾기(안전)
    const postForm =
        qs("#postForm") ||
        qs("#reviewPostForm") ||
        qs("form[method='post'][action*='reviews']") ||
        qs("form");

    // 작성 페이지 아니면 종료
    if (!postForm || !qs("#miniSummary")) return;

    // =========================
    // ✅ CSRF 유틸 (meta는 web/unity 껍데기에 존재)
    // =========================
    function getCsrfHeaders() {
        const tokenMeta = qs('meta[name="_csrf"]');
        const headerMeta = qs('meta[name="_csrf_header"]');
        if (!tokenMeta || !headerMeta) return {};
        return { [headerMeta.content]: tokenMeta.content };
    }

    // =========================
    // ✅ Quill 초기화 + 이미지 업로드
    // =========================
    let quill = null;

    const editorEl = qs("#editor");
    const contentHidden = qs("#content");

    // Quill 라이브러리가 로드되어 있고 editor가 있으면 초기화
    if (editorEl && window.Quill) {
        const toolbarOptions = [
            [{ header: [1, 2, 3, false] }],
            ["bold", "italic", "underline", "strike"],
            [{ color: [] }, { background: [] }],
            [{ list: "ordered" }, { list: "bullet" }],
            [{ align: [] }],
            ["blockquote", "code-block"],
            ["link", "image"],
            ["clean"]
        ];

        function imageHandler() {
            const input = document.createElement("input");
            input.type = "file";
            input.accept = "image/*";
            input.click();

            input.onchange = async () => {
                const file = input.files && input.files[0];
                if (!file) return;

                // 1차 용량 제한(5MB)
                const maxSize = 5 * 1024 * 1024;
                if (file.size > maxSize) {
                    alert("이미지는 최대 5MB까지 업로드할 수 있어요.");
                    return;
                }

                try {
                    const formData = new FormData();
                    formData.append("image", file);

                    const res = await fetch("/reviews/upload-image", {
                        method: "POST",
                        headers: { ...getCsrfHeaders() },
                        body: formData
                    });

                    if (!res.ok) {
                        const err = await res.json().catch(() => ({}));
                        alert(err.error || "이미지 업로드에 실패했습니다.");
                        return;
                    }

                    const data = await res.json();
                    const imageUrl = data.url;

                    if (!imageUrl) {
                        alert("서버 응답에 url이 없습니다.");
                        return;
                    }

                    // 커서 위치에 삽입
                    const range = quill.getSelection(true);
                    const index = range ? range.index : quill.getLength();

                    quill.insertEmbed(index, "image", imageUrl, "user");
                    quill.insertText(index + 1, "\n", "user");
                    quill.setSelection(index + 2, 0);

                } catch (e) {
                    console.error(e);
                    alert("업로드 중 오류가 발생했습니다.");
                }
            };
        }

        quill = new Quill("#editor", {
            theme: "snow",
            placeholder: "여행 중 느낀 점, 팁, 추천 코스를 자유롭게 작성해 주세요",
            modules: {
                toolbar: {
                    container: toolbarOptions,
                    handlers: { image: imageHandler }
                }
            }
        });

        // 서버 validation 실패로 다시 돌아왔을 때 contentHidden 값이 있으면 복원
        if (contentHidden && contentHidden.value && contentHidden.value.trim().length > 0) {
            quill.root.innerHTML = contentHidden.value;
        }
    }

    // =========================
    // ✅ 필수 태그 검증
    // =========================
    function validateRequiredTags() {
        const missing = [];

        const travelType = getHiddenValue("travelType");
        const theme = getHiddenValue("theme");
        const period = getHiddenValue("period");
        const level = getHiddenValue("level");

        const regions = qsa('input[name="regionTags"]')
            .map((inp) => (inp.value || "").trim())
            .filter((v) => v.length > 0);

        if (!travelType) missing.push("여행 유형");
        if (!theme) missing.push("테마");
        if (!period) missing.push("기간");
        if (!level) missing.push("난이도");
        if (regions.length === 0) missing.push("지역");

        if (missing.length > 0) {
            alert(`모든 카테고리 태그를 1개 이상 선택해야 합니다.\n\n미선택: ${missing.join(", ")}`);
            return false;
        }
        return true;
    }

    // =========================
    // 미리보기(요약 바)
    // =========================
    function renderMiniSummary() {
        const box = qs("#miniSummary");
        if (!box) return;

        box.innerHTML = "";

        const typeMap = { solo: "혼자", couple: "커플", family: "가족", friends: "친구" };
        const themeMap = { freedom: "자유여행", healing: "힐링", food: "맛집", activity: "액티비티", nature: "자연" };

        const addChip = (label, className = "") => {
            const text = (label ?? "").toString().trim();
            if (!text) return;
            const span = document.createElement("span");
            span.className = `chip ${className}`.trim();
            span.textContent = text;
            box.appendChild(span);
        };

        const chips = [];

        const travelType = getHiddenValue("travelType");
        if (travelType) chips.push(typeMap[travelType] || travelType);

        const theme = getHiddenValue("theme");
        if (theme) chips.push(themeMap[theme] || theme);

        const period = getHiddenValue("period");
        if (period) chips.push(period);

        const level = getHiddenValue("level");
        if (level) chips.push(level);

        qsa('input[name="regionTags"]').forEach((inp) => {
            const v = (inp.value || "").trim();
            if (v) chips.push(v);
        });

        if (chips.length === 0) {
            const empty = document.createElement("span");
            empty.className = "empty";
            empty.textContent = "선택된 조건 없음";
            box.appendChild(empty);
        } else {
            chips.forEach((label) => addChip(label));
        }

        const flight = readBudget("budgetFlight");
        const lodging = readBudget("budgetLodging") || readBudget("budgetStay");
        const food = readBudget("budgetFood");
        const extra = readBudget("budgetExtra") || readBudget("budgetEtc");

        const total = flight + lodging + food + extra;

        addChip(`항공 ${fmt(flight)}만`);
        addChip(`숙박 ${fmt(lodging)}만`);
        addChip(`식비 ${fmt(food)}만`);
        addChip(`기타 ${fmt(extra)}만`);
        addChip(`총액 ${fmt(total)}만 원`, "budget");

        const totalHidden = qs("#budgetTotal");
        if (totalHidden) totalHidden.value = String(total);
    }

    // =========================
    // 칩 선택 로직 (단일 + 지역 다중)
    // =========================
    qsa(".filter-chips").forEach((group) => {
        const key = group.dataset.key;               // travelType/theme/period/level/region
        const mode = group.dataset.mode || "single"; // single | multi

        const row = group.closest(".filter-row");
        const singleHidden =
            row?.querySelector(`input[name="${key}"]`) || qs(`input[name="${key}"]`);

        group.addEventListener("click", (e) => {
            const btn = e.target.closest(".chip");
            if (!btn) return;

            const value = btn.dataset.value;

            // ✅ 단일 선택
            if (mode === "single") {
                qsa(".chip", group).forEach((c) => c.classList.remove("active"));
                btn.classList.add("active");

                if (singleHidden) singleHidden.value = value;
                renderMiniSummary();
                return;
            }

            // ✅ 다중(region)
            if (mode === "multi") {
                btn.classList.toggle("active");

                const wrap =
                    row?.querySelector(".region-inputs") ||
                    qs(".region-inputs") ||
                    postForm;

                // 기존 regionTags hidden 제거
                qsa('input[name="regionTags"]', wrap).forEach((el) => el.remove());

                // 선택된 칩을 hidden으로 추가
                qsa(".chip.active", group).forEach((chip) => {
                    const input = document.createElement("input");
                    input.type = "hidden";
                    input.name = "regionTags";
                    input.value = chip.dataset.value;
                    wrap.appendChild(input);
                });

                renderMiniSummary();
            }
        });
    });

    // =========================
    // 예산 입력: 숫자만 + 즉시 요약 갱신
    // =========================
    qsa(".b-input").forEach((inp) => {
        inp.addEventListener("input", () => {
            inp.value = onlyNumber(inp.value);
            renderMiniSummary();
        });
    });

    // =========================
    // 최초 1회 렌더
    // =========================
    renderMiniSummary();

    // =========================
    // ✅ submit 처리
    // - 태그 검증
    // - Quill 내용 HTML -> hidden(content)
    // =========================
    postForm.addEventListener("submit", (e) => {
        if (!validateRequiredTags()) {
            e.preventDefault();
            e.stopPropagation();
            return;
        }

        // ✅ quill이 있으면 content에 저장
        if (quill && contentHidden) {
            contentHidden.value = quill.root.innerHTML;
        }
    });
});
