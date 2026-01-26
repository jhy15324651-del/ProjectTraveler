// reviews.js
document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("filterForm");
    if (!form) return;

    // ==============================
    // multi tag hidden input 생성 영역 매핑
    // ==============================
    const multiWrap = {
        period: { wrapId: "periodInputs", inputName: "periodTags" },
        // budget은 슬라이더로 변경했으므로 budgetTags는 사용하지 않음
        level:  { wrapId: "levelInputs",  inputName: "levelTags" },
        region: { wrapId: "regionInputs", inputName: "regionTags" }
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
    // 미니 요약(태그 칩) 렌더링
    // ==============================
    function renderMiniSummary() {
        const box = document.getElementById("miniSummary");
        if (!box) return;

        // 1) 기존 내용 비우기
        box.innerHTML = "";

        // 2) 라벨 매핑 (single 값 -> 한글 표시)
        const typeMap = {
            solo: "혼자",
            couple: "커플",
            family: "가족",
            friends: "친구",
        };

        const themeMap = {
            healing: "힐링",
            food: "맛집",
            activity: "액티비티",
            nature: "자연",
        };

        // 3) 칩 생성 유틸
        const addChip = (label, className = "") => {
            const span = document.createElement("span");
            span.className = `chip ${className}`.trim();
            span.textContent = label;
            box.appendChild(span);
        };

        // 4) 선택값 모으기
        const chips = [];

        // (A) 단일: 여행유형
        const typeVal = document.getElementById("f_type")?.value || "";
        if (typeVal) chips.push(typeMap[typeVal] || typeVal);

        // (B) 단일: 테마
        const themeVal = document.getElementById("f_theme")?.value || "";
        if (themeVal) chips.push(themeMap[themeVal] || themeVal);

        // (C) 복수: 기간
        document.querySelectorAll("#periodInputs input[type='hidden']").forEach((inp) => {
            const v = (inp.value || "").trim();
            if (!v) return;
            chips.push(v.includes("_") ? v.split("_").slice(1).join("_") : v); // d_2박3일 -> 2박3일
        });

        // (D) 복수: 난이도
        document.querySelectorAll("#levelInputs input[type='hidden']").forEach((inp) => {
            const v = (inp.value || "").trim();
            if (!v) return;
            chips.push(v.includes("_") ? v.split("_").slice(1).join("_") : v); // lv_초급 -> 초급
        });

        // (E) 복수: 지역
        document.querySelectorAll("#regionInputs input[type='hidden']").forEach((inp) => {
            const v = (inp.value || "").trim();
            if (!v) return;
            chips.push(v.includes("_") ? v.split("_").slice(1).join("_") : v); // r_도쿄 -> 도쿄
        });

        // 5) 일반 칩들 먼저 출력
        if (chips.length === 0) {
            const empty = document.createElement("span");
            empty.className = "empty";
            empty.textContent = "선택된 조건 없음";
            box.appendChild(empty);
        } else {
            chips.forEach((label) => addChip(label));
        }

        // 6) 예산은 무조건 아래줄(항상 마지막에 추가)
        const min = parseInt(document.getElementById("f_budgetMin")?.value || "0", 10);
        const max = parseInt(document.getElementById("f_budgetMax")?.value || "5000000", 10);

        const fmt = (n) => (isNaN(n) ? "0" : n).toLocaleString("ko-KR");
        const budgetText = `예산 범위 : ${fmt(min)}원 ~ ${fmt(max)}원`;
        addChip(budgetText, "budget");
    }

    // ==============================
    // 칩 클릭 핸들러 (필터 선택)
    // ==============================
    document.querySelectorAll(".filter-chips").forEach(group => {
        const key = group.dataset.key;     // type/theme/period/level/region
        const mode = group.dataset.mode;   // single / multi-or / multi-toggle

        group.addEventListener("click", (e) => {
            const btn = e.target.closest(".chip");
            if (!btn) return;

            const val = btn.dataset.value;

            // 1) "전체" 클릭
            if (val === "") {
                clearGroupActive(group);
                btn.classList.add("active");

                // single hidden 처리
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
    // ==============================
    const minInput  = document.getElementById("budgetMin");
    const maxInput  = document.getElementById("budgetMax");
    const minLabel  = document.getElementById("budgetMinLabel");
    const maxLabel  = document.getElementById("budgetMaxLabel");
    const hiddenMin = document.getElementById("f_budgetMin");
    const hiddenMax = document.getElementById("f_budgetMax");

    // ✅ 50만원 단위 구분선(ticks) 컨테이너
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

        // ✅ 선택 구간 하이라이트 갱신
        if (rangeBar) {
            const min = parseInt(minInput.min, 10);
            const max = parseInt(minInput.max, 10);

            const left  = ((minVal - min) / (max - min)) * 100;
            const right = ((maxVal - min) / (max - min)) * 100;

            rangeBar.style.left  = left + "%";
            rangeBar.style.width = (right - left) + "%";
        }

        // ✅ 예산 변경 시 요약도 즉시 갱신
        renderMiniSummary();
    }

    // ✅ 50만원 단위 구분선 + 100만원 단위 라벨
    function renderBudgetTicks() {
        if (!ticksWrap || !minInput) return;

        const min = parseInt(minInput.min, 10);   // 0
        const max = parseInt(minInput.max, 10);   // 5000000
        const tickStep  = 500000;                 // 구분선 50만
        const labelStep = 1000000;                // 라벨 100만

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

        // ✅ 0도 라벨 포함
        addTick(min, true);

        // ✅ 50만 단위 tick, 100만 단위 라벨(끝 500만 포함)
        for (let v = tickStep; v <= max; v += tickStep) {
            const withLabel = (v % labelStep === 0);
            addTick(v, withLabel);
        }
    }

    if (minInput && maxInput) {
        minInput.addEventListener("input", syncBudget);
        maxInput.addEventListener("input", syncBudget);

        syncBudget();         // 초기 표시 (+ 요약 갱신 포함)
        renderBudgetTicks();  // 구분선 렌더링
    }

    // ==============================
    // ✅ 최초 1회 렌더 (칩/예산 포함)
    // ==============================
    renderMiniSummary();
});
