// reviews.js
// =====================================================
// Reviews Filter UI Script
// - chips(single/multi) + hidden inputs sync + page reset
// - budget: min/max slider + range bar + ticks + reset/unlock
// - URL 파라미터로 UI 복원 (페이지네이션 이동 시 유지)
// - Region: 상위(그룹) -> 하위(세부칩) 드롭다운
// - Search/Paging: postList로 스무스 스크롤
// =====================================================

document.addEventListener("DOMContentLoaded", () => {
    const filterForm = document.getElementById("filterForm");
    if (!filterForm) return;

// =====================================================
// 0) Multi hidden input mapping
// =====================================================
    const multiWrap = {
        period: { wrapId: "periodInputs", inputName: "periods" },
        theme:  { wrapId: "themeInputs",  inputName: "themes"  }, // ✅ 추가
        level:  { wrapId: "levelInputs",  inputName: "levels"  },
        region: { wrapId: "tagInputs",    inputName: "tags"    },
    };

    const params = new URLSearchParams(window.location.search);

    // =====================================================
    // 1) Utils (공용 함수)
    // =====================================================
    const isBlank = (v) => v == null || String(v).trim() === "";
    const toIntOrNull = (v) => {
        if (isBlank(v)) return null;
        const n = parseInt(v, 10);
        return Number.isFinite(n) ? n : null;
    };

    function clearGroupActive(group) {
        group.querySelectorAll(".chip").forEach((c) => c.classList.remove("active"));
    }

    // ✅ "전체" 칩 활성화: 기본은 data-value="" 기반
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

    /**
     * multi 그룹(period/level/region) 선택값을 hidden input 리스트로 동기화
     * - values가 비면 wrap 내부 비움
     */
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

    // =====================================================
    // 2) Mini Summary (선택된 조건 요약)
    // =====================================================
    function renderMiniSummary() {
        const box = document.getElementById("miniSummary");
        if (!box) return;

        box.innerHTML = "";

        const typeMap  = { solo: "🧍혼자", couple: "💕커플", family: "👨‍👩‍👧‍👦가족", friends: "🧑‍🤝‍🧑친구" , withBaby: "👶아이 동반", peoples: "🧑‍🤝‍🧑‍단체"};
        const themeMap = { freedom: "🕊️자유여행", healing: "🌿힐링", food: "🍣️맛집", activity: "🎢액티비티", nature: "🌲자연", shopping:"🛍️쇼핑" };

        const addChip = (label, className = "") => {
            const text = (label ?? "").toString().trim();
            if (!text) return;

            const span = document.createElement("span");
            span.className = `chip ${className}`.trim();
            span.textContent = text;
            box.appendChild(span);
        };

        const chips = [];

        // (1) single
        const typeVal = document.getElementById("f_travelType")?.value || "";
        if (typeVal) chips.push(typeMap[typeVal] || typeVal);

        // ✅ themes (multi) - hidden inputs 기반
        document.querySelectorAll("#themeInputs input[type='hidden']").forEach((inp) => {
            const v = (inp.value || "").trim();
            if (!v) return;
            chips.push(themeMap[v] || v);
        });

        // (2) multi (hidden inputs 기반)
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

        // (3) UI 렌더
        if (chips.length === 0) {
            const empty = document.createElement("span");
            empty.className = "empty";
            empty.textContent = "선택된 조건 없음";
            box.appendChild(empty);
        } else {
            chips.forEach((label) => addChip(label));
        }

        // (4) budget 요약 (항상 표시)
        const min = parseInt(document.getElementById("f_minBudget")?.value || "0", 10);
        const max = parseInt(document.getElementById("f_maxBudget")?.value || "5000000", 10);
        const fmt = (n) => (isNaN(n) ? "0" : n).toLocaleString("ko-KR");
        addChip(`예산 범위 : ${fmt(min)}원 ~ ${fmt(max)}원`, "budget");
    }

    // =====================================================
    // 3) URL -> UI 복원 (칩 active + hidden inputs)
    // =====================================================
    function restoreSingle(key, paramName) {
        const group = document.querySelector(`.filter-chips[data-key="${key}"]`);
        if (!group) return;

        const val = params.get(paramName) || "";

        const hidden = document.getElementById("f_" + key);
        if (hidden) hidden.value = val;

        clearGroupActive(group);
        const btn =
            group.querySelector(`.chip[data-value="${val}"]`) ||
            group.querySelector(`.chip[data-value=""]`);
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

    // =====================================================
    // 4) Budget slider (HTML ID 완전 일치)
    // =====================================================
    const minInput  = document.getElementById("budgetMin");
    const maxInput  = document.getElementById("budgetMax");
    const minLabel  = document.getElementById("budgetMinLabel");
    const maxLabel  = document.getElementById("budgetMaxLabel");

    const hiddenMin = document.getElementById("f_minBudget");
    const hiddenMax = document.getElementById("f_maxBudget");

    const ticksWrap = document.getElementById("budgetTicks");
    const rangeBar  = document.getElementById("budgetRange");

    // 버튼/상태 hidden (HTML 기준)
    const resetBtn      = document.getElementById("budgetResetBtn");
    const unlockBtn     = document.getElementById("budgetUnlockBtn"); // HTML: budgetUnlockBtn
    const hiddenUnlimit = document.getElementById("f_budgetUnlimit"); // hidden 상태

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
    }

    // tick step 자동 계산(대략 10칸)
    function calcTickStep(max) {
        const rough = max / 10;
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

        if (!Number.isFinite(minVal)) minVal = DEFAULT_MIN;
        if (!Number.isFinite(maxVal)) maxVal = parseInt(maxInput.max || String(DEFAULT_MAX), 10);

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
            const curMin = parseInt(minInput.value || "0", 10);
            const curMax = parseInt(maxInput.value || "0", 10);

            if (curMin > DEFAULT_MAX) minInput.value = String(DEFAULT_MAX);
            if (curMax > DEFAULT_MAX) maxInput.value = String(DEFAULT_MAX);
        }

        renderBudgetTicks();
        syncBudget({ reset: false });
    }

    // =====================================================
    // 5) Region Group Dropdown (main -> sub)  ✅ 먼저 선언(초기화에서 사용)
    // =====================================================
    const regionSubWrap  = document.getElementById("regionSubWrap");
    const regionSubLabel = document.getElementById("regionSubLabel");
    const regionSubGroup = document.querySelector(".filter-chips.region-sub[data-key='region']");
    const regionSubChips = document.querySelectorAll(".filter-chips.region-sub .chip");
    const regionMainBtns = document.querySelectorAll(".region-main-btn");

    function closeRegionDropdown() {
        if (regionSubWrap)  regionSubWrap.style.display  = "none";
        if (regionSubLabel) regionSubLabel.style.display = "none";
        regionSubChips.forEach((ch) => (ch.style.display = "none"));
    }

    function openRegionDropdown(group) {
        if (!regionSubWrap) return;

        regionSubWrap.style.display = "block";
        if (regionSubLabel) regionSubLabel.style.display = "block";

        regionSubChips.forEach((ch) => {
            const g = ch.getAttribute("data-group");
            ch.style.display = (g === group) ? "" : "none";
        });
    }

    function setRegionMainActive(group) {
        regionMainBtns.forEach((b) => b.classList.remove("active"));
        const target = [...regionMainBtns].find((b) => (b.getAttribute("data-group") || "") === (group || ""));
        if (target) target.classList.add("active");
    }

    /**
     * ✅ "지역만" 초기화 (완전)
     * - 하위 active 해제
     * - hidden tags 제거
     * - 드롭다운/라벨 닫기
     * - 상위 "전체" active
     * - page=0 + summary 갱신
     *
     * ⚠️ 하위(group=data-key="region")에는 "전체" 칩이 HTML에 없으므로
     *     setAllActive(regionSubGroup)는 의미가 없거나 부작용 가능 → 사용하지 않음
     */
    function resetRegionOnly() {
        if (regionSubGroup) {
            clearGroupActive(regionSubGroup);
        }

        syncMultiHiddenInputs("region", []);
        closeRegionDropdown();

        setRegionMainActive("");

        resetPage();
        renderMiniSummary();
    }

    // =====================================================
    // 6) 초기 로드: URL 기반 복원 + budget + summary
    // =====================================================
    restoreSingle("travelType", "travelType");

    restoreMulti("theme", "themes");
    restoreMulti("period", "periods");
    restoreMulti("level", "levels");
    restoreMulti("region", "tags");

    // Region: 첫 태그 기준으로 상위칩/드롭다운 자동 오픈
    closeRegionDropdown();
    const initialTags = document.querySelectorAll("#tagInputs input[type='hidden']");
    if (initialTags.length > 0) {
        const firstTag = (initialTags[0].value || "").trim();
        if (firstTag) {
            const firstChip = [...regionSubChips].find((ch) => ch.dataset.value === firstTag);
            const g = firstChip?.getAttribute("data-group");
            if (g) {
                openRegionDropdown(g);
                setRegionMainActive(g);
            }
        }
    } else {
        setRegionMainActive("");
    }

    // budget unlock 상태 (hidden 값을 신뢰)
    const unlockedInit = isUnlocked();
    setUnlocked(unlockedInit);
    applyMaxLimit(unlockedInit, { clamp: false });

    // budget 값: 숫자일 때만 반영
    if (minInput && maxInput) {
        const minQ = toIntOrNull(params.get("minBudget"));
        const maxQ = toIntOrNull(params.get("maxBudget"));
        if (minQ !== null) minInput.value = String(minQ);
        if (maxQ !== null) maxInput.value = String(maxQ);
    }

    renderBudgetTicks();
    syncBudget({ reset: false });

    // page/size 복원(있으면)
    const pageHidden = document.getElementById("f_page");
    const sizeHidden = document.getElementById("f_size");
    if (pageHidden && params.get("page") !== null) pageHidden.value = params.get("page");
    if (sizeHidden && params.get("size") !== null) sizeHidden.value = params.get("size");

    // =====================================================
    // 7) Chip click handlers (single / multi 공통)
    // - regionMain(상위) / region(하위)는 별도 로직이 있으니 제외
    // =====================================================
    document.querySelectorAll(".filter-chips").forEach((group) => {
        const key = group.dataset.key;
        const mode = group.dataset.mode;

        if (!key || !mode) return;
        if (key === "regionMain") return; // ✅ 상위 지역 제외
        if (key === "region") return;     // ✅ 하위 지역 제외(아래 전용 핸들러)

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

    // =====================================================
    // 8) Region events (main -> sub + sub multi)
    // =====================================================
    // 8-1) 상위지역 버튼 클릭 핸들링
    regionMainBtns.forEach((btn) => {
        btn.addEventListener("click", () => {
            const group = btn.getAttribute("data-group") || "";

            setRegionMainActive(group);

            // 상위 "전체" 클릭 = 지역만 초기화
            if (group === "") {
                resetRegionOnly();
                return;
            }

            openRegionDropdown(group);
        });
    });

    // 8-2) 세부지역 칩 클릭 (multi-toggle)
    if (regionSubGroup) {
        regionSubGroup.addEventListener("click", (e) => {
            const btn = e.target.closest(".chip");
            if (!btn) return;

            // 하위칩은 data-value가 반드시 있음(HTML 기준)
            const val = (btn.getAttribute("data-value") || "").trim();
            if (!val) return;

            btn.classList.toggle("active");

            const selected = [...regionSubGroup.querySelectorAll(".chip.active")]
                .map((c) => (c.getAttribute("data-value") || "").trim())
                .filter((v) => v);

            syncMultiHiddenInputs("region", selected);

            resetPage();
            renderMiniSummary();
        });
    }

    // =====================================================
    // 9) Budget events (slider + buttons)
    // =====================================================
    if (minInput && maxInput) {
        minInput.addEventListener("input", () => syncBudget({ reset: true }));
        maxInput.addEventListener("input", () => syncBudget({ reset: true }));
    }

    if (resetBtn) {
        resetBtn.addEventListener("click", () => {
            if (!minInput || !maxInput) return;

            const curMax = parseInt(maxInput.max, 10);
            minInput.value = String(DEFAULT_MIN);
            maxInput.value = String(curMax);

            syncBudget({ reset: true });
        });
    }

    if (unlockBtn) {
        unlockBtn.setAttribute("aria-pressed", isUnlocked() ? "true" : "false");

        unlockBtn.addEventListener("click", () => {
            const next = !isUnlocked();
            setUnlocked(next);
            applyMaxLimit(next, { clamp: true });
            resetPage();
        });
    }

    // =====================================================
    // 10) 전체 초기화 버튼 (검색 버튼 오른쪽)
    // - 화면/hidden/요약만 초기화 (submit 안함)
    // ✅ region은 resetRegionOnly()로 완전 초기화
    // =====================================================
    const allResetBtn = document.getElementById("btnReset");

    if (allResetBtn) {
        allResetBtn.addEventListener("click", () => {
            // 1) page 0
            resetPage();

            // 2) single 그룹: 전체로 + hidden 비우기
            ["travelType"].forEach((key) => {
                const group = document.querySelector(`.filter-chips[data-key="${key}"]`);
                if (!group) return;

                clearGroupActive(group);
                setAllActive(group);

                const hidden = document.getElementById("f_" + key);
                if (hidden) hidden.value = "";
            });

            // 3) multi 그룹(period/level): 전체로 + hidden list 비우기
            ["period", "theme", "level"].forEach((key) => {
                const group = document.querySelector(`.filter-chips[data-key="${key}"]`);
                if (!group) return;

                clearGroupActive(group);
                setAllActive(group);

                syncMultiHiddenInputs(key, []);
            });

            // 3-1) region: 상위/하위/hidden/드롭다운까지 완전 초기화
            resetRegionOnly();

            // 4) budget: 잠금 상태로 + 값 초기화(0~500만)
            setUnlocked(false);
            applyMaxLimit(false, { clamp: false });

            if (minInput && maxInput) {
                minInput.value = String(DEFAULT_MIN);
                maxInput.value = String(DEFAULT_MAX);
            }

            syncBudget({ reset: false });

            // 5) 검색 타입/검색어 초기화
            const qField = document.querySelector(".q-field");
            const qInput = document.querySelector(".q-input");

            if (qField) qField.value = "tc";
            if (qInput) {
                qInput.value = "";
                qInput.textContent = "";
            }

            // 6) 요약 갱신
            renderMiniSummary();

            // (선택) URL까지 깨끗하게 만들려면 submit
            // filterForm.submit();
        });
    }

    // =====================================================
    // 11) Smooth scroll to #postList (search/paging 공통) - FIX
    // =====================================================
    function smoothToPostList() {
        const el = document.getElementById("postList");
        if (!el) return;

        const offset =
            parseInt(
                getComputedStyle(document.querySelector(".reviews") || document.documentElement)
                    .getPropertyValue("--rv-anchor-offset")
            ) || 90;

        const y = el.getBoundingClientRect().top + window.pageYOffset - offset;

        window.scrollTo({ top: Math.max(0, y - 60), behavior: "auto" });
        requestAnimationFrame(() => {
            window.scrollTo({ top: Math.max(0, y), behavior: "smooth" });
        });
    }

    // ✅ 페이지네이션 '#postList' 진입도 스무스 처리
    if (window.location.hash === "#postList") {
        history.replaceState(null, "", window.location.pathname + window.location.search);
        requestAnimationFrame(() => smoothToPostList());
    }

    // ✅ 검색으로 넘어온 경우에도 스무스 스크롤
    if (sessionStorage.getItem("rv_scroll_to_postlist") === "1") {
        sessionStorage.removeItem("rv_scroll_to_postlist");
        requestAnimationFrame(() => smoothToPostList());
    }

    // =====================================================
    // 12) 검색 submit: hash 붙이지 않고, 로드 후 스무스 스크롤만
    // =====================================================
    filterForm.addEventListener("submit", (e) => {
        e.preventDefault();

        sessionStorage.setItem("rv_scroll_to_postlist", "1");

        const url = new URL(window.location.pathname, window.location.origin);
        const fd = new FormData(filterForm);

        for (const [k, v] of fd.entries()) {
            if (v == null) continue;
            const s = String(v).trim();
            if (s === "") continue;
            url.searchParams.append(k, s);
        }

        window.location.href = url.toString();
    });
});
