// reviews.js (복붙용 최적화 완성본)
// DTO 필드: q, travelType/theme, periods(List), levels(List), tags(List), minBudget/maxBudget, page/size

document.addEventListener("DOMContentLoaded", () => {
    const filterForm = document.getElementById("filterForm");
    if (!filterForm) return; // ✅ 리스트 페이지 아닐 때는 즉시 종료

    // ==============================
    // multi hidden input 생성 매핑
    // ==============================
    const multiWrap = {
        period: { wrapId: "periodInputs", inputName: "periods" },
        level:  { wrapId: "levelInputs",  inputName: "levels"  },
        region: { wrapId: "tagInputs",    inputName: "tags"    }
    };

    // ==============================
    // Utils
    // ==============================
    const params = new URLSearchParams(window.location.search);

    function clearGroupActive(group) {
        group.querySelectorAll(".chip").forEach(c => c.classList.remove("active"));
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
            .map(c => c.dataset.value)
            .filter(v => v !== "");
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
        values.forEach(v => {
            const input = document.createElement("input");
            input.type = "hidden";
            input.name = cfg.inputName;
            input.value = v;
            wrap.appendChild(input);
        });
    }

    // ==============================
    // Mini Summary
    // ==============================
    function renderMiniSummary() {
        const box = document.getElementById("miniSummary");
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

        // 단일: travelType/theme
        const typeVal = document.getElementById("f_travelType")?.value || "";
        if (typeVal) chips.push(typeMap[typeVal] || typeVal);

        const themeVal = document.getElementById("f_theme")?.value || "";
        if (themeVal) chips.push(themeMap[themeVal] || themeVal);

        // 다중: periods/levels/tags
        document.querySelectorAll("#periodInputs input[type='hidden']").forEach(inp => {
            const v = (inp.value || "").trim();
            if (v) chips.push(v);
        });

        document.querySelectorAll("#levelInputs input[type='hidden']").forEach(inp => {
            const v = (inp.value || "").trim();
            if (v) chips.push(v);
        });

        document.querySelectorAll("#tagInputs input[type='hidden']").forEach(inp => {
            const v = (inp.value || "").trim();
            if (v) chips.push(v);
        });

        if (chips.length === 0) {
            const empty = document.createElement("span");
            empty.className = "empty";
            empty.textContent = "선택된 조건 없음";
            box.appendChild(empty);
        } else {
            chips.forEach(label => addChip(label));
        }

        // 예산
        const min = parseInt(document.getElementById("f_minBudget")?.value || "0", 10);
        const max = parseInt(document.getElementById("f_maxBudget")?.value || "5000000", 10);
        const fmt = n => (isNaN(n) ? "0" : n).toLocaleString("ko-KR");
        addChip(`예산 범위 : ${fmt(min)}원 ~ ${fmt(max)}원`, "budget");
    }

    // ==============================
    // URL -> UI 복원 (핵심)
    // ==============================
    function restoreSingle(key, paramName) {
        const group = document.querySelector(`.filter-chips[data-key="${key}"]`);
        if (!group) return;

        const val = params.get(paramName) || "";

        // hidden
        const hidden = document.getElementById("f_" + key);
        if (hidden) hidden.value = val;

        // chips
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
        values.forEach(v => {
            const btn = group.querySelector(`.chip[data-value="${v}"]`);
            if (btn) btn.classList.add("active");
        });
    }

    // ==============================
    // Budget slider
    // ==============================
    const minInput  = document.getElementById("budgetMin");
    const maxInput  = document.getElementById("budgetMax");
    const minLabel  = document.getElementById("budgetMinLabel");
    const maxLabel  = document.getElementById("budgetMaxLabel");

    const hiddenMin = document.getElementById("f_minBudget");
    const hiddenMax = document.getElementById("f_maxBudget");

    const ticksWrap = document.getElementById("budgetTicks");
    const rangeBar  = document.getElementById("budgetRange");

    function formatWon(n) {
        return Number(n).toLocaleString("ko-KR") + "원";
    }

    // ✅ 초기 로드에는 resetPage를 하면 안 됨 (page 복원이 깨짐)
    function syncBudget({ reset = false } = {}) {
        if (!minInput || !maxInput) return;

        let minVal = parseInt(minInput.value, 10);
        let maxVal = parseInt(maxInput.value, 10);

        if (minVal > maxVal) {
            minVal = maxVal;
            minInput.value = String(minVal);
        }

        if (minLabel) minLabel.textContent = formatWon(minVal);
        if (maxLabel) maxLabel.textContent = formatWon(maxVal);

        if (hiddenMin) hiddenMin.value = String(minVal);
        if (hiddenMax) hiddenMax.value = String(maxVal);

        if (rangeBar) {
            const min = parseInt(minInput.min, 10);
            const max = parseInt(minInput.max, 10);
            const left  = ((minVal - min) / (max - min)) * 100;
            const right = ((maxVal - min) / (max - min)) * 100;
            rangeBar.style.left  = left + "%";
            rangeBar.style.width = (right - left) + "%";
        }

        if (reset) resetPage();
        renderMiniSummary();
    }

    function renderBudgetTicks() {
        if (!ticksWrap || !minInput) return;

        const min = parseInt(minInput.min, 10);
        const max = parseInt(minInput.max, 10);
        const tickStep  = 500000;   // 50만
        const labelStep = 1000000;  // 100만

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
        for (let v = tickStep; v <= max; v += tickStep) addTick(v, (v % labelStep === 0));
    }

    // ==============================
    // 1) URL 기반 복원 먼저
    // ==============================
    // single
    restoreSingle("travelType", "travelType");
    restoreSingle("theme", "theme");

    // multi
    restoreMulti("period", "periods");
    restoreMulti("level", "levels");
    restoreMulti("region", "tags");

    // budget (URL 값 있으면 range에도 반영)
    if (minInput && maxInput) {
        const minQ = params.get("minBudget");
        const maxQ = params.get("maxBudget");
        if (minQ !== null) minInput.value = minQ;
        if (maxQ !== null) maxInput.value = maxQ;
    }
    syncBudget({ reset: false });
    renderBudgetTicks();

    // page/size 복원 (있을 때만)
    const pageHidden = document.getElementById("f_page");
    const sizeHidden = document.getElementById("f_size");
    if (pageHidden && params.get("page") !== null) pageHidden.value = params.get("page");
    if (sizeHidden && params.get("size") !== null) sizeHidden.value = params.get("size");

    // ==============================
    // 2) 이벤트 바인딩 (사용자 조작 시 page=0)
    // ==============================
    document.querySelectorAll(".filter-chips").forEach(group => {
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

    if (minInput && maxInput) {
        minInput.addEventListener("input", () => syncBudget({ reset: true }));
        maxInput.addEventListener("input", () => syncBudget({ reset: true }));
    }
});
