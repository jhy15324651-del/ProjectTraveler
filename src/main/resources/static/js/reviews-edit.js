// reviews-edit.js (FINAL / copy-paste)
// ✅ 목표: 수정 화면에서
// - 단일칩(travelType/period/level) 복원 + 클릭 동기화
// - 테마(themes) 다중 선택(multi-or) + hidden inputs(#themeInputs) 동기화 + 복원
// - 지역(regionTags) 다중 + initRegionTags(data-tags)로 복원 + 그룹 드롭다운 동작
// - Quill init + 이미지 업로드 + submit 시 hidden(content) 동기화

document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("postForm");
    if (!form) return;

    const idInput = form.querySelector('input[name="id"]');
    if (!idInput || !idInput.value) return; // edit 페이지가 아니면 종료

    // =========================
    // Utils
    // =========================
    const isBlank = (v) => v == null || String(v).trim() === "";
    const qs = (sel, root = document) => root.querySelector(sel);
    const qsa = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    const syncHiddenValueByKey = (key, value) => {
        const input = qs(`input[type="hidden"][name="${key}"]`, form);
        if (input) input.value = value ?? "";
    };

    const setSingleChipActive = (key, value) => {
        const wrap = qs(`.filter-chips[data-key="${key}"][data-mode="single"]`);
        if (!wrap) return;

        qsa("button.chip[data-value]", wrap).forEach((b) => b.classList.remove("active"));

        if (!isBlank(value)) {
            const target = qsa("button.chip[data-value]", wrap).find((b) => b.dataset.value === value);
            if (target) target.classList.add("active");
        }
    };

    // =========================
    // Quill init + image upload (EDIT)
    // =========================
    function getCsrfHeaders() {
        const tokenMeta = qs('meta[name="_csrf"]');
        const headerMeta = qs('meta[name="_csrf_header"]');
        if (!tokenMeta || !headerMeta) return {};
        return { [headerMeta.content]: tokenMeta.content };
    }

    const editorEl = qs("#editor");
    const contentHidden = qs("#content");

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

                    const range = window.quill.getSelection(true);
                    const index = range ? range.index : window.quill.getLength();
                    window.quill.insertEmbed(index, "image", imageUrl, "user");
                    window.quill.insertText(index + 1, "\n", "user");
                    window.quill.setSelection(index + 2, 0);

                } catch (e) {
                    console.error(e);
                    alert("업로드 중 오류가 발생했습니다.");
                }
            };
        }

        window.quill = new Quill("#editor", {
            theme: "snow",
            placeholder: "여행 중 느낀 점, 팁, 추천 코스를 자유롭게 작성해 주세요",
            modules: {
                toolbar: {
                    container: toolbarOptions,
                    handlers: { image: imageHandler }
                }
            }
        });

        // ✅ 기존 본문 HTML 복원(딱 1번만)
        if (contentHidden && contentHidden.value && contentHidden.value.trim().length > 0) {
            window.quill.root.innerHTML = contentHidden.value;
        }
    }

    // =========================
    // themes (multi-or)
    // =========================
    const themeGroup = qs('.filter-chips[data-key="theme"][data-mode="multi-or"]');
    const themeInputsWrap = qs("#themeInputs") || form;

    function rebuildThemeHiddenInputs(values) {
        if (!themeInputsWrap) return;

        qsa('input[name="themes"]', themeInputsWrap).forEach((el) => el.remove());

        (values || []).forEach((v) => {
            if (isBlank(v)) return;
            const inp = document.createElement("input");
            inp.type = "hidden";
            inp.name = "themes";
            inp.value = v;
            themeInputsWrap.appendChild(inp);
        });
    }

    function restoreThemesFromHidden() {
        if (!themeGroup) return;

        const selected = qsa('#themeInputs input[name="themes"]')
            .map((i) => (i.value || "").trim())
            .filter((v) => !isBlank(v));

        qsa(".chip", themeGroup).forEach((c) => c.classList.remove("active"));

        const allBtn = qs('.chip[data-value=""]', themeGroup);

        if (selected.length === 0) {
            if (allBtn) allBtn.classList.add("active");
            return;
        }

        if (allBtn) allBtn.classList.remove("active");

        selected.forEach((v) => {
            const btn = qs(`.chip[data-value="${v}"]`, themeGroup);
            if (btn) btn.classList.add("active");
        });
    }

    // =========================
    // regionTags (group dropdown)
    // =========================
    const regionInputsWrap = qs("#regionInputs");

    const regionSubWrap = qs("#regionSubWrap");
    const regionSubLabel = qs("#regionSubLabel");
    const regionSubChips = qsa(".filter-chips.region-sub button.chip[data-value]");
    const regionMainBtns = qsa(".filter-chips.region-main .region-main-btn");

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
        return raw.split(",").map((s) => s.trim()).filter((s) => !isBlank(s));
    };

    function closeRegionDropdown() {
        if (regionSubWrap) regionSubWrap.style.display = "none";
        if (regionSubLabel) regionSubLabel.style.display = "none";
        regionSubChips.forEach((ch) => (ch.style.display = "none"));
    }

    function openRegionDropdown(group) {
        if (!regionSubWrap) return;
        regionSubWrap.style.display = "";
        if (regionSubLabel) regionSubLabel.style.display = "";

        regionSubChips.forEach((ch) => {
            ch.style.display = (ch.dataset.group === group) ? "" : "none";
        });
    }

    function setRegionMainActive(group) {
        regionMainBtns.forEach((b) => b.classList.remove("active"));
        const target = regionMainBtns.find((b) => (b.dataset.group || "") === (group || ""));
        if (target) target.classList.add("active");
    }

    const applyRegionTagsToUI = (tags) => {
        const tagSet = new Set(tags || []);

        // active 초기화
        regionSubChips.forEach((b) => b.classList.remove("active"));

        // tags active
        regionSubChips.forEach((b) => {
            if (tagSet.has(b.dataset.value)) b.classList.add("active");
        });

        const firstTag = tags && tags.length > 0 ? tags[0] : null;
        if (!firstTag) {
            setRegionMainActive("");
            closeRegionDropdown();
            return;
        }

        const firstBtn = regionSubChips.find((b) => b.dataset.value === firstTag);
        const group = firstBtn?.dataset.group;
        if (!group) return;

        openRegionDropdown(group);
        setRegionMainActive(group);
    };

    // =========================
    // Mini summary
    // =========================
    function updateMiniSummary() {
        const box = qs("#miniSummary");
        if (!box) return;

        box.innerHTML = "";
        const chips = [];

        const travelType = qs('input[type="hidden"][name="travelType"]', form)?.value;
        const period = qs('input[type="hidden"][name="period"]', form)?.value;
        const level = qs('input[type="hidden"][name="level"]', form)?.value;

        if (!isBlank(travelType)) chips.push(travelType);

        qsa('input[type="hidden"][name="themes"]', themeInputsWrap)
            .map((i) => (i.value || "").trim())
            .filter((v) => !isBlank(v))
            .forEach((t) => chips.push(t));

        if (!isBlank(period)) chips.push(period);
        if (!isBlank(level)) chips.push(level);

        qsa('input[type="hidden"][name="regionTags"]', form)
            .map((i) => (i.value || "").trim())
            .filter((v) => !isBlank(v))
            .forEach((t) => chips.push(t));

        if (chips.length === 0) {
            const span = document.createElement("span");
            span.className = "empty";
            span.textContent = "선택된 조건 없음";
            box.appendChild(span);
            return;
        }

        chips.forEach((p) => {
            const s = document.createElement("span");
            s.className = "chip";
            s.textContent = p;
            box.appendChild(s);
        });
    }

    // =========================
    // 1) 초기값 복원(단일)
    // =========================
    const initTravelType = qs("#initTravelType")?.value || "";
    const initPeriod = qs("#initPeriod")?.value || "";
    const initLevel = qs("#initLevel")?.value || "";

    setSingleChipActive("travelType", initTravelType);
    syncHiddenValueByKey("travelType", initTravelType);

    setSingleChipActive("period", initPeriod);
    syncHiddenValueByKey("period", initPeriod);

    setSingleChipActive("level", initLevel);
    syncHiddenValueByKey("level", initLevel);

    // 2) themes 복원
    restoreThemesFromHidden();

    // 3) 지역 복원
    const initTags = getInitialRegionTags();
    rebuildRegionHiddenInputs(initTags);
    applyRegionTagsToUI(initTags);

    // =========================
    // 4) 단일칩 클릭
    // =========================
    qsa('.filter-chips[data-mode="single"]').forEach((wrap) => {
        const key = wrap.dataset.key;

        wrap.addEventListener("click", (e) => {
            const btn = e.target.closest("button.chip[data-value]");
            if (!btn) return;

            qsa("button.chip[data-value]", wrap).forEach((b) => b.classList.remove("active"));
            btn.classList.add("active");

            syncHiddenValueByKey(key, btn.dataset.value);
            updateMiniSummary();
        });
    });

    // =========================
    // 5) 테마(multi-or) 클릭
    // =========================
    if (themeGroup) {
        themeGroup.addEventListener("click", (e) => {
            const btn = e.target.closest(".chip");
            if (!btn) return;

            const value = (btn.dataset.value || "").trim();
            const allBtn = qs('.chip[data-value=""]', themeGroup);

            // 전체 클릭
            if (value === "") {
                qsa(".chip", themeGroup).forEach((c) => c.classList.remove("active"));
                btn.classList.add("active");
                rebuildThemeHiddenInputs([]);
                updateMiniSummary();
                return;
            }

            // 전체 비활성
            if (allBtn) allBtn.classList.remove("active");

            // 토글
            btn.classList.toggle("active");

            const selected = qsa(".chip.active", themeGroup)
                .map((c) => (c.dataset.value || "").trim())
                .filter((v) => !isBlank(v) && v !== "");

            // 아무것도 없으면 전체
            if (selected.length === 0) {
                qsa(".chip", themeGroup).forEach((c) => c.classList.remove("active"));
                if (allBtn) allBtn.classList.add("active");
                rebuildThemeHiddenInputs([]);
                updateMiniSummary();
                return;
            }

            rebuildThemeHiddenInputs(selected);
            updateMiniSummary();
        });
    }

    // =========================
    // 6) 지역 상위 그룹 클릭
    // =========================
    regionMainBtns.forEach((btn) => {
        btn.addEventListener("click", () => {
            const group = btn.dataset.group || "";

            setRegionMainActive(group);

            // ↺(전체) 누르면 지역만 초기화
            if (isBlank(group)) {
                regionSubChips.forEach((b) => {
                    b.classList.remove("active");
                    b.style.display = "none";
                });
                rebuildRegionHiddenInputs([]);
                closeRegionDropdown();
                updateMiniSummary();
                return;
            }

            openRegionDropdown(group);
        });
    });

    // =========================
    // 7) 지역 하위칩 클릭
    // =========================
    regionSubChips.forEach((btn) => {
        btn.addEventListener("click", () => {
            btn.classList.toggle("active");

            const selected = regionSubChips
                .filter((b) => b.classList.contains("active"))
                .map((b) => b.dataset.value)
                .filter((v) => !isBlank(v));

            rebuildRegionHiddenInputs(selected);
            updateMiniSummary();
        });
    });

    // =========================
    // 8) submit 동기화
    // =========================
    form.addEventListener("submit", () => {
        // Quill -> content
        if (window.quill) {
            const html = window.quill.root.innerHTML;
            const contentInput = qs("#content");
            if (contentInput) contentInput.value = html;
        }

        // regionTags 비면 initTags라도 유지(안전장치)
        const regionHidden = qsa('input[type="hidden"][name="regionTags"]', form)
            .map((i) => i.value)
            .filter((v) => !isBlank(v));

        if (regionHidden.length === 0 && initTags.length > 0) {
            rebuildRegionHiddenInputs(initTags);
        }
    });

    // =========================
    // 최초 요약 렌더
    // =========================
    updateMiniSummary();
});