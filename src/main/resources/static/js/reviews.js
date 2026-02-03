// reviews.js
// - chips(single/multi) + hidden inputs + 페이지 reset
// - budget: min/max slider + range bar + ticks + reset/unlock
// - URL 파라미터로 UI 복원 (페이지네이션 이동 시 유지)

document.addEventListener("DOMContentLoaded", () => {
    const filterForm = document.getElementById("filterForm");
    if (!filterForm) return;

    // ==============================
    // Multi hidden input mapping
    // ==============================
    const multiWrap = {
        period: { wrapId: "periodInputs", inputName: "periods" },
        level:  { wrapId: "levelInputs",  inputName: "levels"  },
        region: { wrapId: "tagInputs",    inputName: "tags"    }
    };

    const params = new URLSearchParams(window.location.search);

    // ==============================
    // Utils
    // ==============================
    const isBlank = (v) => v == null || String(v).trim() === "";
    const toIntOrNull = (v) => {
        if (isBlank(v)) return null;
        const n = parseInt(v, 10);
        return Number.isFinite(n) ? n : null;
    };

    function clearGroupActive(group) {
        group.querySelectorAll(".chip").forEach((c) => c.classList.remove("active"));
    }
    function setAllActive(group) {
        const allBtn = group.querySelector('.chip[data-value=""]');
        if (allBtn) allBtn.classList.add("active");
    }
    function unsetAllActive(group) {
        const allBtn = group.querySelector('.chip[data-value=""]');
        if (allBtn) allBtn.classList.remove("active");
    }
    function getSelectedValues(group) {
        return [...group.querySelectorAll(".chip.active")]
            .map((c) => c.dataset.value)
            .filter((v) => v !== "");
    }
    function resetPage() {
        const pageInput = document.getElementById("f_page");
        if (pageInput) pageInput.value = "0";
    }
    function syncMultiHiddenInputs(key, values) {
        const cfg = multiWrap[key];
        if (!cfg) return;
        const wrap = document.getElementById(cfg.wrapId);
        if (!wrap) return;

        wrap.innerHTML = "";
        values.forEach((v) => {
            const input = document.createElement("input");
            input.type = "hidden";
            input.name = cfg.inputName;
            input.value = v;
            wrap.appendChild(input);
        });
    }

    // ==============================
    // Mini Summary (선택된 조건 요약)
    // ==============================
    function renderMiniSummary() {
        const box = document.getElementById("miniSummary");
        if (!box) return;

        box.innerHTML = "";

        const typeMap  = { solo: "혼자", couple: "커플", family: "가족", friends: "친구" };
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

        // single
        const typeVal = document.getElementById("f_travelType")?.value || "";
        if (typeVal) chips.push(typeMap[typeVal] || typeVal);

        const themeVal = document.getElementById("f_theme")?.value || "";
        if (themeVal) chips.push(themeMap[themeVal] || themeVal);

        // multi
        document.querySelectorAll("#periodInputs input[type='hidden']").forEach((inp) => {
            const v = (inp.value || "").trim();
            if (v) chips.push(v);
        });
        document.querySelectorAll("#levelInputs input[type='hidden']").forEach((inp) => {
            const v = (inp.value || "").trim();
            if (v) chips.push(v);
        });
        document.querySelectorAll("#tagInputs input[type='hidden']").forEach((inp) => {
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

        // budget
        const min = parseInt(document.getElementById("f_minBudget")?.value || "0", 10);
        const max = parseInt(document.getElementById("f_maxBudget")?.value || "5000000", 10);
        const fmt = (n) => (isNaN(n) ? "0" : n).toLocaleString("ko-KR");
        addChip(`예산 범위 : ${fmt(min)}원 ~ ${fmt(max)}원`, "budget");
    }

    // ==============================
    // URL -> UI 복원
    // ==============================
    function restoreSingle(key, paramName) {
        const group = document.querySelector(`.filter-chips[data-key="${key}"]`);
        if (!group) return;

        const val = params.get(paramName) || "";

        const hidden = document.getElementById("f_" + key);
        if (hidden) hidden.value = val;

        clearGroupActive(group);
        const btn = group.querySelector(`.chip[data-value="${val}"]`) || group.querySelector(`.chip[data-value=""]`);
        if (btn) btn.classList.add("active");
    }

    function restoreMulti(key, paramName) {
        const group = document.querySelector(`.filter-chips[data-key="${key}"]`);
        if (!group) return;

        const values = params.getAll(paramName) || [];
        syncMultiHiddenInputs(key, values);

        clearGroupActive(group);
        if (values.length === 0) {
            setAllActive(group);
            return;
        }

        unsetAllActive(group);
        values.forEach((v) => {
            const btn = group.querySelector(`.chip[data-value="${v}"]`);
            if (btn) btn.classList.add("active");
        });
    }

    // ==============================
    // Budget slider (HTML ID 완전 일치)
    // ==============================
    const minInput  = document.getElementById("budgetMin");
    const maxInput  = document.getElementById("budgetMax");
    const minLabel  = document.getElementById("budgetMinLabel");
    const maxLabel  = document.getElementById("budgetMaxLabel");

    const hiddenMin = document.getElementById("f_minBudget");
    const hiddenMax = document.getElementById("f_maxBudget");

    const ticksWrap = document.getElementById("budgetTicks");
    const rangeBar  = document.getElementById("budgetRange");

    // 버튼/상태 hidden (HTML 기준)
    const resetBtn   = document.getElementById("budgetResetBtn");
    const unlockBtn  = document.getElementById("budgetUnlockBtn");     // ✅ 핵심: HTML은 UnlockBtn
    const hiddenUnlimit = document.getElementById("f_budgetUnlimit");  // ✅ hidden 상태

    // 기본값 / 제한해제 max
    const DEFAULT_MIN = 0;
    const DEFAULT_MAX = 5000000;
    const UNLIMIT_MAX = 50000000; // 5천만 (원하면 조절)

    function formatWon(n) {
        return Number(n).toLocaleString("ko-KR") + "원";
    }

    function isUnlocked() {
        return (hiddenUnlimit?.value || "0") === "1";
    }

    function setUnlocked(on) {
        if (hiddenUnlimit) hiddenUnlimit.value = on ? "1" : "0";
        if (unlockBtn) unlockBtn.setAttribute("aria-pressed", on ? "true" : "false");
        // 버튼 텍스트는 취향 (원하면 유지)
        // unlockBtn.textContent = on ? "제한해제됨" : "제한해제";
    }

    // tick step 자동 계산(대략 10칸)
    function calcTickStep(max) {
        // max=5,000,000이면 500,000 정도 나오게
        const rough = max / 10;
        // 보기 좋게 10만 단위로 반올림
        return Math.max(100000, Math.round(rough / 100000) * 100000);
    }

    function renderBudgetTicks() {
        if (!ticksWrap || !minInput || !maxInput) return;

        const min = parseInt(minInput.min, 10);
        const max = parseInt(maxInput.max, 10);

        const tickStep  = calcTickStep(max);
        const labelStep = tickStep * 2;

        ticksWrap.innerHTML = "";

        const toManLabel = (v) => (v === 0 ? "0" : `${Math.round(v / 10000)}만`);

        const addTick = (value, withLabel) => {
            const left = ((value - min) / (max - min)) * 100;

            const tick = document.createElement("span");
            tick.className = "tick";
            tick.style.left = left + "%";
            ticksWrap.appendChild(tick);

            if (withLabel) {
                const label = document.createElement("span");
                label.className = "tick-label";
                label.style.left = left + "%";
                label.textContent = toManLabel(value);
                ticksWrap.appendChild(label);
            }
        };

        addTick(min, true);
        for (let v = min + tickStep; v <= max; v += tickStep) {
            addTick(v, (v % labelStep === 0));
        }
    }

    function syncBudget({ reset = false } = {}) {
        if (!minInput || !maxInput) return;

        let minVal = parseInt(minInput.value, 10);
        let maxVal = parseInt(maxInput.value, 10);

        // NaN 방지
        if (!Number.isFinite(minVal)) minVal = DEFAULT_MIN;
        if (!Number.isFinite(maxVal)) maxVal = parseInt(maxInput.max || String(DEFAULT_MAX), 10);

        // 교차 방지
        if (minVal > maxVal) {
            minVal = maxVal;
            minInput.value = String(minVal);
        }

        // 라벨
        if (minLabel) minLabel.textContent = formatWon(minVal);
        if (maxLabel) maxLabel.textContent = formatWon(maxVal);

        // hidden
        if (hiddenMin) hiddenMin.value = String(minVal);
        if (hiddenMax) hiddenMax.value = String(maxVal);

        // range bar (%는 maxInput.max 기준)
        if (rangeBar) {
            const min = parseInt(minInput.min, 10);
            const max = parseInt(maxInput.max, 10);
            const left  = ((minVal - min) / (max - min)) * 100;
            const right = ((maxVal - min) / (max - min)) * 100;
            rangeBar.style.left  = left + "%";
            rangeBar.style.width = (right - left) + "%";
        }

        if (reset) resetPage();
        renderMiniSummary();
    }

    function applyMaxLimit(unlocked, { clamp = true } = {}) {
        if (!minInput || !maxInput) return;

        const newMax = unlocked ? UNLIMIT_MAX : DEFAULT_MAX;

        minInput.max = String(newMax);
        maxInput.max = String(newMax);

        if (clamp && !unlocked) {
            // 잠금으로 돌아갈 때 현재 값이 500만 넘으면 500만으로 clamp
            const curMin = parseInt(minInput.value || "0", 10);
            const curMax = parseInt(maxInput.value || "0", 10);
            if (curMin > DEFAULT_MAX) minInput.value = String(DEFAULT_MAX);
            if (curMax > DEFAULT_MAX) maxInput.value = String(DEFAULT_MAX);
        }

        renderBudgetTicks();
        syncBudget({ reset: false });
    }

    // ==============================
    // 1) URL 기반 복원 먼저
    // ==============================
    restoreSingle("travelType", "travelType");
    restoreSingle("theme", "theme");

    restoreMulti("period", "periods");
    restoreMulti("level", "levels");
    restoreMulti("region", "tags");

    // budget unlock 상태 (URL param or hidden)
    // - HTML에서 th:value="${param.budgetUnlimit}" 로 들어오므로 hidden 값을 신뢰
    const unlockedInit = isUnlocked();
    setUnlocked(unlockedInit);
    applyMaxLimit(unlockedInit, { clamp: false });

    // budget 값: 숫자일 때만 반영 (빈값이면 유지)
    if (minInput && maxInput) {
        const minQ = toIntOrNull(params.get("minBudget"));
        const maxQ = toIntOrNull(params.get("maxBudget"));
        if (minQ !== null) minInput.value = String(minQ);
        if (maxQ !== null) maxInput.value = String(maxQ);
    }

    // ticks + sync
    renderBudgetTicks();
    syncBudget({ reset: false });

    // page/size 복원(있으면)
    const pageHidden = document.getElementById("f_page");
    const sizeHidden = document.getElementById("f_size");
    if (pageHidden && params.get("page") !== null) pageHidden.value = params.get("page");
    if (sizeHidden && params.get("size") !== null) sizeHidden.value = params.get("size");

    // ==============================
    // 2) 이벤트 바인딩 (사용자 조작 시 page=0)
    // ==============================
    document.querySelectorAll(".filter-chips").forEach((group) => {
        const key = group.dataset.key;
        const mode = group.dataset.mode;

        group.addEventListener("click", (e) => {
            const btn = e.target.closest(".chip");
            if (!btn) return;

            const val = btn.dataset.value;

            // (1) 전체
            if (val === "") {
                clearGroupActive(group);
                btn.classList.add("active");

                if (mode === "single") {
                    const hidden = document.getElementById("f_" + key);
                    if (hidden) hidden.value = "";
                } else if (mode.startsWith("multi")) {
                    syncMultiHiddenInputs(key, []);
                }

                resetPage();
                renderMiniSummary();
                return;
            }

            // (2) 단일
            if (mode === "single") {
                clearGroupActive(group);
                btn.classList.add("active");

                const hidden = document.getElementById("f_" + key);
                if (hidden) hidden.value = val;

                resetPage();
                renderMiniSummary();
                return;
            }

            // (3) 다중
            unsetAllActive(group);
            btn.classList.toggle("active");

            const selected = getSelectedValues(group);

            if (selected.length === 0) {
                clearGroupActive(group);
                setAllActive(group);
                syncMultiHiddenInputs(key, []);
                resetPage();
                renderMiniSummary();
                return;
            }

            syncMultiHiddenInputs(key, selected);
            resetPage();
            renderMiniSummary();
        });
    });

    // budget sliders
    if (minInput && maxInput) {
        minInput.addEventListener("input", () => syncBudget({ reset: true }));
        maxInput.addEventListener("input", () => syncBudget({ reset: true }));
    }

    // ==============================
    // Budget buttons
    // ==============================
    if (resetBtn) {
        resetBtn.addEventListener("click", () => {
            if (!minInput || !maxInput) return;

            const curMax = parseInt(maxInput.max, 10); // 잠금/해제 상태에 따른 현재 max
            minInput.value = String(DEFAULT_MIN);
            maxInput.value = String(curMax);

            syncBudget({ reset: true });
        });
    }

    if (unlockBtn) {
        // 초기 aria
        unlockBtn.setAttribute("aria-pressed", isUnlocked() ? "true" : "false");

        unlockBtn.addEventListener("click", () => {
            const next = !isUnlocked();
            setUnlocked(next);
            applyMaxLimit(next, { clamp: true });

            resetPage();
        });
    }
});
