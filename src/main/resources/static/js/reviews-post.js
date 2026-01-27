// reviews-post.js
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

    // =========================
    // 미리보기(요약 바)
    // =========================
    function renderMiniSummary() {
        const box = document.getElementById("miniSummary");
        if (!box) return;

        box.innerHTML = "";

        // 표시 라벨 매핑
        const typeMap = { solo: "혼자", couple: "커플", family: "가족", friends: "친구" };
        const themeMap = { freedom: "자유여행", healing: "힐링", food: "맛집", activity: "액티비티", nature: "자연" };

        const addChip = (label, className = "") => {
            const span = document.createElement("span");
            span.className = `chip ${className}`.trim();
            span.textContent = label;
            box.appendChild(span);
        };

        const chips = [];

        // ✅ 단일 태그(hidden input 값 읽기)
        const travelType = getHiddenValue("travelType");
        if (travelType) chips.push(typeMap[travelType] || travelType);

        const theme = getHiddenValue("theme");
        if (theme) chips.push(themeMap[theme] || theme);

        const period = getHiddenValue("period");
        if (period) chips.push(period);

        const level = getHiddenValue("level");
        if (level) chips.push(level);

        // ✅ 지역(다중): regionTags hidden inputs
        qsa('input[name="regionTags"]').forEach((inp) => {
            const v = (inp.value || "").trim();
            if (v) chips.push(v);
        });

        // 1) 일반 칩 출력
        if (chips.length === 0) {
            const empty = document.createElement("span");
            empty.className = "empty";
            empty.textContent = "선택된 조건 없음";
            box.appendChild(empty);
        } else {
            chips.forEach((label) => addChip(label));
        }

        // =========================
        // ✅ 예산(항공/숙박/식비/기타) + 총액
        //  - 항목 4개는 같은 줄(일반칩)
        //  - 총액만 아랫줄(budget)
        // =========================
        const flight = readBudget("budgetFlight");
        const stay = readBudget("budgetStay");
        const food = readBudget("budgetFood");
        const etc = readBudget("budgetEtc");
        const total = flight + stay + food + etc;

        // 항목 4개는 항상 노출(0도 표시)
        addChip(`항공 ${fmt(flight)}만`);
        addChip(`숙박 ${fmt(stay)}만`);
        addChip(`식비 ${fmt(food)}만`);
        addChip(`기타 ${fmt(etc)}만`);

        // 총액은 항상 마지막 줄
        addChip(`총액 ${fmt(total)}만 원`, "budget");

        // 서버로 총액도 같이 보내고 싶다면 hidden 갱신
        const totalHidden = document.getElementById("budgetTotal");
        if (totalHidden) totalHidden.value = String(total);
    }

    // =========================
    // 칩 선택 로직 (단일 + 지역 다중)
    // =========================
    qsa(".filter-chips").forEach((group) => {
        const key = group.dataset.key;               // travelType/theme/period/level/region
        const mode = group.dataset.mode || "single"; // single | multi (region)

        const row = group.closest(".filter-row");
        const singleHidden = row?.querySelector(`input[name="${key}"]`);
        const multiWrap = row?.querySelector(".region-inputs");

        group.addEventListener("click", (e) => {
            const btn = e.target.closest(".chip");
            if (!btn) return;

            const value = btn.dataset.value;

            // 단일 선택
            if (mode === "single") {
                qsa(".chip", group).forEach((c) => c.classList.remove("active"));
                btn.classList.add("active");

                if (singleHidden) singleHidden.value = value;

                renderMiniSummary();
                return;
            }

            // 지역: 다중 선택
            if (mode === "multi") {
                btn.classList.toggle("active");

                if (multiWrap) {
                    multiWrap.innerHTML = "";

                    qsa(".chip.active", group).forEach((chip) => {
                        const input = document.createElement("input");
                        input.type = "hidden";
                        input.name = "regionTags";
                        input.value = chip.dataset.value;
                        multiWrap.appendChild(input);
                    });
                }

                renderMiniSummary();
            }
        });
    });

    // =========================
    // 예산 입력: 숫자만 + 즉시 요약 갱신
    // =========================
    const budgetInputs = qsa(".b-input"); // ✅ HTML에서 예산 input들에 class="b-input" 붙인 기준
    budgetInputs.forEach((inp) => {
        inp.addEventListener("input", () => {
            inp.value = onlyNumber(inp.value);
            renderMiniSummary();
        });
    });

    // =========================
    // 최초 1회 렌더
    // =========================
    renderMiniSummary();
});
