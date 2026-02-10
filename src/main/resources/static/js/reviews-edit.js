// reviews-edit.js
// 목적: 수정 화면 진입 시 기존 데이터 UI 복원 + 제출 시 content/regionTags 세팅
// 전제:
// - edit.html에 init 힌트 존재: #initTravelType/#initTheme/#initPeriod/#initLevel
// - edit.html에 regionTags 힌트 존재: #initRegionTags[data-tags="도쿄,오사카"]
// - Quill editor: #editor, hidden input: #content
// - 지역 hidden inputs container: #regionInputs
// - form: #postForm

document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("postForm");
    if (!form) return;

    // edit 화면에서만 동작하도록 가드(원하면 더 엄격히 체크 가능)
    const idInput = form.querySelector('input[name="id"]');
    if (!idInput || !idInput.value) return; // id 없으면 edit 아님

    // ---------- Utils ----------
    const isBlank = (v) => v == null || String(v).trim() === "";
    const qs = (sel, root = document) => root.querySelector(sel);
    const qsa = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    const setSingleChipActive = (key, value) => {
        if (isBlank(value)) return;

        const wrap = qs(`.filter-chips[data-key="${key}"][data-mode="single"]`);
        if (!wrap) return;

        const btns = qsa(`button.chip[data-value]`, wrap);
        btns.forEach((b) => b.classList.remove("active"));

        const target = btns.find((b) => b.dataset.value === value);
        if (target) target.classList.add("active");

        // hidden input(th:field)도 값 동기화 (안 해도 서버 값은 있으나, UI 변경 후 일관성 위해)
        const hidden = wrap.parentElement?.querySelector(`input[type="hidden"][name="${key}"], input[type="hidden"]`);
        // 위 selector는 프로젝트마다 name이 travelType/theme...으로 들어가므로 보수적으로 처리
    };

    const syncHiddenValueByKey = (key, value) => {
        // th:field="*{travelType}" -> name="travelType" 로 렌더됨
        const input = qs(`input[type="hidden"][name="${key}"]`, form);
        if (input) input.value = value ?? "";
    };

    // regionTags hidden inputs 생성: name="regionTags"
    const regionInputsWrap = qs("#regionInputs");
    const rebuildRegionHiddenInputs = (tags) => {
        if (!regionInputsWrap) return;
        regionInputsWrap.innerHTML = "";
        (tags || []).forEach((t) => {
            if (isBlank(t)) return;
            const inp = document.createElement("input");
            inp.type = "hidden";
            inp.name = "regionTags";
            inp.value = t;
            regionInputsWrap.appendChild(inp);
        });
    };

    const getInitialRegionTags = () => {
        const box = qs("#initRegionTags");
        if (!box) return [];
        const raw = box.getAttribute("data-tags") || "";
        if (isBlank(raw)) return [];
        return raw
            .split(",")
            .map((s) => s.trim())
            .filter((s) => !isBlank(s));
    };

    // 지역 chip UI 활성화 + 표시 처리
    const applyRegionTagsToUI = (tags) => {
        const tagSet = new Set(tags || []);

        // 1) 모든 sub chip active 제거
        qsa(".filter-chips.region-sub button.chip[data-value]").forEach((b) => {
            b.classList.remove("active");
        });

        // 2) tags에 해당하는 chip active
        qsa(".filter-chips.region-sub button.chip[data-value]").forEach((b) => {
            if (tagSet.has(b.dataset.value)) b.classList.add("active");
        });

        // 3) 어떤 그룹을 열어야 하는지: 선택된 태그 중 첫 번째 태그의 data-group 기준으로 sub 목록 표시
        const firstTag = tags && tags.length > 0 ? tags[0] : null;
        if (!firstTag) return;

        const firstBtn = qsa(".filter-chips.region-sub button.chip[data-value]").find(
            (b) => b.dataset.value === firstTag
        );
        const group = firstBtn?.dataset.group;

        if (!group) return;

        // sub wrap 표시
        const subWrap = qs("#regionSubWrap");
        const subLabel = qs("#regionSubLabel");
        if (subWrap) subWrap.style.display = "";
        if (subLabel) subLabel.style.display = "";

        // 해당 group에 속한 버튼만 show
        qsa(".filter-chips.region-sub button.chip[data-value]").forEach((b) => {
            b.style.display = b.dataset.group === group ? "" : "none";
        });

        // main group chip도 active 표시
        qsa(".filter-chips.region-main button.region-main-btn").forEach((b) => b.classList.remove("active"));
        const mainBtn = qsa(".filter-chips.region-main button.region-main-btn").find(
            (b) => b.dataset.group === group
        );
        if (mainBtn) mainBtn.classList.add("active");
    };

    // mini summary 갱신(있으면)
    const updateMiniSummary = () => {
        const box = qs("#miniSummary");
        if (!box) return;

        const parts = [];

        const travelType = qs('input[type="hidden"][name="travelType"]')?.value;
        const theme = qs('input[type="hidden"][name="theme"]')?.value;
        const period = qs('input[type="hidden"][name="period"]')?.value;
        const level = qs('input[type="hidden"][name="level"]')?.value;

        if (!isBlank(travelType)) parts.push(travelType);
        if (!isBlank(theme)) parts.push(theme);
        if (!isBlank(period)) parts.push(period);
        if (!isBlank(level)) parts.push(level);

        // regionTags
        const regionTags = qsa('input[type="hidden"][name="regionTags"]', form).map((i) => i.value).filter((v) => !isBlank(v));
        regionTags.forEach((t) => parts.push(t));

        box.innerHTML = "";
        if (parts.length === 0) {
            const span = document.createElement("span");
            span.className = "empty";
            span.textContent = "선택된 조건 없음";
            box.appendChild(span);
        } else {
            parts.forEach((p) => {
                const s = document.createElement("span");
                s.className = "chip"; // 기존 스타일 재사용 (없으면 CSS에서 tag-summary-mini 전용 클래스로 바꿔도 됨)
                s.textContent = p;
                box.appendChild(s);
            });
        }
    };

    // ---------- 1) 초기값 복원(단일 칩) ----------
    const initTravelType = qs("#initTravelType")?.value || "";
    const initTheme = qs("#initTheme")?.value || "";
    const initPeriod = qs("#initPeriod")?.value || "";
    const initLevel = qs("#initLevel")?.value || "";

    // UI active + hidden 값 동기화
    setSingleChipActive("travelType", initTravelType);
    syncHiddenValueByKey("travelType", initTravelType);

    setSingleChipActive("theme", initTheme);
    syncHiddenValueByKey("theme", initTheme);

    setSingleChipActive("period", initPeriod);
    syncHiddenValueByKey("period", initPeriod);

    setSingleChipActive("level", initLevel);
    syncHiddenValueByKey("level", initLevel);

    // ---------- 2) 초기값 복원(지역 태그) ----------
    const initTags = getInitialRegionTags();
    rebuildRegionHiddenInputs(initTags);
    applyRegionTagsToUI(initTags);

    // ---------- 3) Quill 초기값 주입 ----------
    // 전제: quill 인스턴스가 전역(예: window.quill)로 생성되어 있거나,
    // 혹은 이 파일에서 생성하는 방식 중 하나여야 함.
    // 여기서는 "이미 페이지에서 Quill이 생성되어 window.quill로 존재한다"는 가정으로 처리.
    const hiddenContent = qs("#content")?.value || "";
    if (window.quill && !isBlank(hiddenContent)) {
        // dangerouslyPasteHTML로 초기 HTML 넣기
        window.quill.clipboard.dangerouslyPasteHTML(hiddenContent);
    }

    // ---------- 4) 칩 클릭 시 hidden 동기화(단일) ----------
    qsa('.filter-chips[data-mode="single"]').forEach((wrap) => {
        const key = wrap.dataset.key;
        qsa("button.chip[data-value]", wrap).forEach((btn) => {
            btn.addEventListener("click", () => {
                // active 토글
                qsa("button.chip[data-value]", wrap).forEach((b) => b.classList.remove("active"));
                btn.classList.add("active");

                // hidden update
                syncHiddenValueByKey(key, btn.dataset.value);

                updateMiniSummary();
            });
        });
    });

    // ---------- 5) 지역 메인 그룹 클릭 ----------
    const mainWrap = qs(".filter-chips.region-main");
    if (mainWrap) {
        qsa("button.region-main-btn", mainWrap).forEach((btn) => {
            btn.addEventListener("click", () => {
                const group = btn.dataset.group || "";

                // active 표시
                qsa("button.region-main-btn", mainWrap).forEach((b) => b.classList.remove("active"));
                btn.classList.add("active");

                const subWrap = qs("#regionSubWrap");
                const subLabel = qs("#regionSubLabel");

                if (isBlank(group)) {
                    // ↺ 전체 초기화
                    if (subWrap) subWrap.style.display = "none";
                    if (subLabel) subLabel.style.display = "none";

                    // sub chip hide + active 제거
                    qsa(".filter-chips.region-sub button.chip[data-value]").forEach((b) => {
                        b.classList.remove("active");
                        b.style.display = "none";
                    });

                    rebuildRegionHiddenInputs([]);
                    updateMiniSummary();
                    return;
                }

                // sub 표시
                if (subWrap) subWrap.style.display = "";
                if (subLabel) subLabel.style.display = "";

                // group 필터
                qsa(".filter-chips.region-sub button.chip[data-value]").forEach((b) => {
                    b.style.display = b.dataset.group === group ? "" : "none";
                });
            });
        });
    }

    // ---------- 6) 지역 서브칩 클릭(다중) ----------
    const subWrap = qs(".filter-chips.region-sub");
    if (subWrap) {
        qsa("button.chip[data-value]", subWrap).forEach((btn) => {
            btn.addEventListener("click", () => {
                btn.classList.toggle("active");

                const selected = qsa("button.chip.active[data-value]", subWrap).map((b) => b.dataset.value);
                rebuildRegionHiddenInputs(selected);
                updateMiniSummary();
            });
        });
    }

    // ---------- 7) 제출 시: Quill -> hidden content 동기화 + regionTags 보정 ----------
    form.addEventListener("submit", () => {
        if (window.quill) {
            const html = window.quill.root.innerHTML;
            const contentInput = qs("#content");
            if (contentInput) contentInput.value = html;
        }

        // regionTags가 하나도 없으면(혹시 UI/JS 깨짐), initTags라도 유지
        const regionHidden = qsa('input[type="hidden"][name="regionTags"]', form).map((i) => i.value).filter((v) => !isBlank(v));
        if (regionHidden.length === 0 && initTags.length > 0) {
            rebuildRegionHiddenInputs(initTags);
        }
    });

    // 마지막으로 요약 갱신
    updateMiniSummary();
});
