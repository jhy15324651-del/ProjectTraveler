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

    // ✅ 작성 폼 찾기(너 프로젝트에서 가장 안전한 방식)
    const postForm =
        qs("#postForm") ||
        qs("#reviewPostForm") ||
        qs("form[method='post'][action*='reviews']") ||
        qs("form");

    // 작성 페이지가 아니라면 그냥 종료
    // (작성 페이지에만 miniSummary/칩이 존재할 것이므로)
    if (!qs("#miniSummary") || !postForm) return;

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
        const box = document.getElementById("miniSummary");
        if (!box) return;

        box.innerHTML = "";

        const typeMap = { solo: "혼자", couple: "커플", family: "가족", friends: "친구" };
        const themeMap = { freedom: "자유여행", healing: "힐링", food: "맛집", activity: "액티비티", nature: "자연" };

        const addChip = (label, className = "") => {
            const text = (label ?? "").toString().trim();
            if (!text) return; // ✅ 빈칩 방지
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

        // ✅ 숙박: budgetLodging(서비스) or budgetStay(기존)
        const lodging = readBudget("budgetLodging") || readBudget("budgetStay");

        // ✅ 식비
        const food = readBudget("budgetFood");

        // ✅ 기타: budgetExtra(서비스) or budgetEtc(기존)
        const extra = readBudget("budgetExtra") || readBudget("budgetEtc");

        const total = flight + lodging + food + extra;


        addChip(`항공 ${fmt(flight)}만`);
        addChip(`숙박 ${fmt(lodging)}만`);
        addChip(`식비 ${fmt(food)}만`);
        addChip(`기타 ${fmt(extra)}만`);
        addChip(`총액 ${fmt(total)}만 원`, "budget");


        const totalHidden = document.getElementById("budgetTotal");
        if (totalHidden) totalHidden.value = String(total);
    }

    // =========================
    // 칩 선택 로직 (단일 + 지역 다중)
    // =========================
    qsa(".filter-chips").forEach((group) => {
        const key = group.dataset.key;               // travelType/theme/period/level/region
        const mode = group.dataset.mode || "single"; // single | multi

        // 단일 hidden은 "같은 row 안"에서 찾되, 없으면 전체에서 fallback
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

            // ✅ 다중(region 등)
            if (mode === "multi") {
                btn.classList.toggle("active");

                // regionTags hidden input들을 갱신: wrapper가 있으면 거기, 없으면 form 안에서 관리
                const wrap =
                    row?.querySelector(".region-inputs") ||
                    qs("#regionInputs") ||
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
    // ✅ submit 차단 (작성 페이지 전용)
    // =========================
    postForm.addEventListener("submit", (e) => {
        if (!validateRequiredTags()) {
            e.preventDefault();
            e.stopPropagation();
        }
    });
});
