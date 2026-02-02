// reviews.js (최종 완전본 - ReviewPostSearchRequest DTO 기준)
// DTO 필드: q, tags(List), minBudget, maxBudget, page, size

document.addEventListener("DOMContentLoaded", () => {
    const filterForm = document.getElementById("filterForm");
    if (!filterForm) return; // ✅ 리스트 페이지 아닐 때는 즉시 종료

    // ==============================
    // multi tag hidden input 생성 영역 매핑
    // - period/level/region은 UI 요약용으로만 유지
    // - 실제 서버 전송은 region -> tags(List<String>)로만 보냄
    // ==============================
    const multiWrap = {
        period: { wrapId: "periodInputs", inputName: null },     // UI용(서버 전송 X)
        level:  { wrapId: "levelInputs",  inputName: null },     // UI용(서버 전송 X)
        region: { wrapId: "tagInputs",    inputName: "tags" }    // ✅ 서버 전송 O (List<String> tags)
    };

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

    // ✅ key(period/level/region)에 따라 hidden inputs 동기화
    // - inputName이 null이면 name 없이 만들어서 서버로 전송되지 않게 함
    function syncMultiHiddenInputs(key, values) {
        const cfg = multiWrap[key];
        if (!cfg) return;

        const wrap = document.getElementById(cfg.wrapId);
        if (!wrap) return;

        wrap.innerHTML = "";
        values.forEach(v => {
            const input = document.createElement("input");
            input.type = "hidden";

            if (cfg.inputName) {
                input.name = cfg.inputName; // ✅ region만 name="tags"
            }
            input.value = v;
            wrap.appendChild(input);
        });
    }

    // ==============================
    // 미니 요약(태그 칩) 렌더링
    // ==============================
    function renderMiniSummary() {
        const box = document.getElementById("miniSummary");
        if (!box) return;

        box.innerHTML = "";

        const typeMap = {
            solo: "혼자",
            couple: "커플",
            family: "가족",
            friends: "친구",
        };

        const themeMap = {
            freedom: "자유여행",
            healing: "힐링",
            food: "맛집",
            activity: "액티비티",
            nature: "자연",
        };

        const addChip = (label, className = "") => {
            const text = (label ?? "").toString().trim();
            if (!text) return;

            const span = document.createElement("span");
            span.className = `chip ${className}`.trim();
            span.textContent = text;
            box.appendChild(span);
        };

        const chips = [];

        // (A) 단일: 여행유형 (UI 요약용)
        const typeVal = document.getElementById("f_type")?.value || "";
        if (typeVal) chips.push(typeMap[typeVal] || typeVal);

        // (B) 단일: 테마 (UI 요약용)
        const themeVal = document.getElementById("f_theme")?.value || "";
        if (themeVal) chips.push(themeMap[themeVal] || themeVal);

        // (C) 복수: 기간 (UI 요약용)
        document.querySelectorAll("#periodInputs input[type='hidden']").forEach((inp) => {
            const v = (inp.value || "").trim();
            if (!v) return;
            chips.push(v.includes("_") ? v.split("_").slice(1).join("_") : v);
        });

        // (D) 복수: 난이도 (UI 요약용)
        document.querySelectorAll("#levelInputs input[type='hidden']").forEach((inp) => {
            const v = (inp.value || "").trim();
            if (!v) return;
            chips.push(v.includes("_") ? v.split("_").slice(1).join("_") : v);
        });

        // (E) 복수: 지역 → ✅ tagInputs (UI 요약 + 서버 전송 tags)
        document.querySelectorAll("#tagInputs input[type='hidden']").forEach((inp) => {
            const v = (inp.value || "").trim();
            if (!v) return;
            chips.push(v.includes("_") ? v.split("_").slice(1).join("_") : v);
        });

        if (chips.length === 0) {
            const empty = document.createElement("span");
            empty.className = "empty";
            empty.textContent = "선택된 조건 없음";
            box.appendChild(empty);
        } else {
            chips.forEach((label) => addChip(label));
        }

        // (F) 예산 (DTO: minBudget/maxBudget) - 항상 마지막
        const min = parseInt(document.getElementById("f_minBudget")?.value || "0", 10);
        const max = parseInt(document.getElementById("f_maxBudget")?.value || "5000000", 10);

        const fmt = (n) => (isNaN(n) ? "0" : n).toLocaleString("ko-KR");
        addChip(`예산 범위 : ${fmt(min)}원 ~ ${fmt(max)}원`, "budget");
    }

    // ==============================
    // 칩 클릭 핸들러 (필터 선택)
    // ==============================
    document.querySelectorAll(".filter-chips").forEach(group => {
        const key = group.dataset.key;   // type/theme/period/level/region
        const mode = group.dataset.mode; // single / multi-or / multi-toggle

        group.addEventListener("click", (e) => {
            const btn = e.target.closest(".chip");
            if (!btn) return;

            const val = btn.dataset.value;

            // 1) "전체" 클릭
            if (val === "") {
                clearGroupActive(group);
                btn.classList.add("active");

                // single hidden 처리 (UI 요약용)
                if (mode === "single") {
                    const hidden = document.getElementById("f_" + key);
                    if (hidden) hidden.value = "";
                }

                // multi hidden 처리
                if (mode.startsWith("multi")) {
                    syncMultiHiddenInputs(key, []);
                }

                renderMiniSummary();
                return;
            }

            // 2) 단일 선택
            if (mode === "single") {
                clearGroupActive(group);
                btn.classList.add("active");

                const hidden = document.getElementById("f_" + key);
                if (hidden) hidden.value = val;

                renderMiniSummary();
                return;
            }

            // 3) 다중 선택
            unsetAllActive(group);
            btn.classList.toggle("active");

            const selected = getSelectedValues(group);

            // 아무것도 선택 안 하면 전체로 복귀
            if (selected.length === 0) {
                clearGroupActive(group);
                setAllActive(group);
                syncMultiHiddenInputs(key, []);
                renderMiniSummary();
                return;
            }

            syncMultiHiddenInputs(key, selected);
            renderMiniSummary();
        });
    });

    // ==============================
    // 예산 슬라이더 (듀얼 range)
    // - hidden: f_minBudget / f_maxBudget (DTO 매칭)
    // ==============================
    const minInput  = document.getElementById("budgetMin");
    const maxInput  = document.getElementById("budgetMax");
    const minLabel  = document.getElementById("budgetMinLabel");
    const maxLabel  = document.getElementById("budgetMaxLabel");

    const hiddenMin = document.getElementById("f_minBudget"); // ✅ 변경
    const hiddenMax = document.getElementById("f_maxBudget"); // ✅ 변경

    const ticksWrap = document.getElementById("budgetTicks");
    const rangeBar  = document.getElementById("budgetRange");

    function formatWon(n) {
        return Number(n).toLocaleString("ko-KR") + "원";
    }

    function syncBudget() {
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

        // 선택 구간 하이라이트
        if (rangeBar) {
            const min = parseInt(minInput.min, 10);
            const max = parseInt(minInput.max, 10);

            const left  = ((minVal - min) / (max - min)) * 100;
            const right = ((maxVal - min) / (max - min)) * 100;

            rangeBar.style.left  = left + "%";
            rangeBar.style.width = (right - left) + "%";
        }

        renderMiniSummary();
    }

    // 50만 단위 구분선 + 100만 단위 라벨
    function renderBudgetTicks() {
        if (!ticksWrap || !minInput) return;

        const min = parseInt(minInput.min, 10);   // 0
        const max = parseInt(minInput.max, 10);   // 5000000
        const tickStep  = 500000;                 // 50만
        const labelStep = 1000000;                // 100만

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

        for (let v = tickStep; v <= max; v += tickStep) {
            addTick(v, (v % labelStep === 0));
        }
    }

    if (minInput && maxInput) {
        minInput.addEventListener("input", syncBudget);
        maxInput.addEventListener("input", syncBudget);

        syncBudget();
        renderBudgetTicks();
    }

    // ==============================
    // 최초 1회 렌더
    // ==============================
    renderMiniSummary();
});
