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
    window.quill = null;

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
        const period = getHiddenValue("period");
        const level = getHiddenValue("level");

        const themes = qsa('input[name="themes"]')
            .map((inp) => (inp.value || "").trim())
            .filter((v) => v.length > 0);

        const regions = qsa('input[name="regionTags"]')
            .map((inp) => (inp.value || "").trim())
            .filter((v) => v.length > 0);

        if (!travelType) missing.push("여행 유형");
        if (themes.length === 0) missing.push("테마");
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

        qsa('input[name="themes"]').forEach((inp) => {
            const v = (inp.value || "").trim();
            if (v) chips.push(themeMap[v] || v);
        });

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
// ✅ Region Group Dropdown (main -> sub) for POST page
// =========================
    const regionSubWrap  = qs("#regionSubWrap");
    const regionSubGroup = qs(".filter-chips.region-sub[data-key='region']");
    const regionSubChips = qsa(".filter-chips.region-sub .chip");
    const regionMainBtns = qsa(".region-main-btn");

// regionTags hidden들이 들어가는 곳
    const regionInputsWrap = qs("#regionInputs") || qs(".region-inputs") || postForm;
    const regionSubLabel = qs("#regionSubLabel"); // ✅ 추가 (없으면 스크립트 죽음)

    function closeRegionDropdown() {
        if (regionSubWrap) regionSubWrap.style.display = "none";
        if (regionSubLabel) regionSubLabel.style.display = "none";
        regionSubChips.forEach((ch) => (ch.style.display = "none"));
    }

    function openRegionDropdown(group) {
        if (!regionSubWrap) return;

        regionSubWrap.style.display = "block";
        if (regionSubLabel) regionSubLabel.style.display = "block"; // ✅ 추가

        regionSubChips.forEach((ch) => {
            const g = ch.getAttribute("data-group");
            ch.style.display = (g === group) ? "" : "none";
        });
    }

    function setRegionMainActive(group) {
        regionMainBtns.forEach((b) => b.classList.remove("active"));
        const target = regionMainBtns.find((b) => (b.getAttribute("data-group") || "") === (group || ""));
        if (target) target.classList.add("active");
    }

    /**
     * ✅ 작성 페이지용 "지역만" 초기화
     * - 하위칩 active 제거
     * - regionTags hidden 제거
     * - 드롭다운 닫기
     * - 요약 갱신
     */
    function resetRegionOnly() {
        // 1) 하위칩 active 제거
        if (regionSubGroup) {
            qsa(".chip", regionSubGroup).forEach((c) => c.classList.remove("active"));
        }

        // 2) regionTags hidden 제거
        qsa('input[name="regionTags"]', regionInputsWrap).forEach((el) => el.remove());

        // 3) 드롭다운 닫기
        closeRegionDropdown();

        // 4) 상위 '전체' 활성
        setRegionMainActive("");

        // 5) 요약 갱신
        renderMiniSummary();
    }

// 1) 상위 지역 버튼 클릭 -> 드롭다운 제어
    regionMainBtns.forEach((btn) => {
        btn.addEventListener("click", () => {
            const group = btn.getAttribute("data-group") || "";

            // 상위칩 active 표시
            setRegionMainActive(group);

            // 상위 '전체' = 지역만 초기화
            if (group === "") {
                resetRegionOnly();
                return;
            }

            // 해당 그룹의 세부 태그만 열기
            openRegionDropdown(group);
        });
    });

// 2) 초기 로드: 이미 선택된 regionTags가 있으면 그 그룹을 열어준다.
    const initialRegionTags = qsa('input[name="regionTags"]', regionInputsWrap)
        .map((inp) => (inp.value || "").trim())
        .filter((v) => v.length > 0);

    if (initialRegionTags.length > 0) {
        const firstTag = initialRegionTags[0];
        const firstChip = regionSubChips.find((ch) => (ch.dataset.value || "") === firstTag);
        const g = firstChip?.getAttribute("data-group");
        if (g) {
            openRegionDropdown(g);
            setRegionMainActive(g);
            // 선택된 그룹의 칩만 보이게 열렸으니 OK
        }
    } else {
        setRegionMainActive("");
        closeRegionDropdown();
    }

    // =========================
    // 칩 선택 로직 (단일 + 지역 다중)
    // =========================
    qsa(".filter-chips").forEach((group) => {
        const key = group.dataset.key;   // travelType/theme/period/level/region
        const mode = group.dataset.mode; // single | multi

        // ✅ region-main 처럼 data-key/data-mode 없는 그룹은 무시
        if (!key || !mode) return;

        const row = group.closest(".filter-row");
        const singleHidden =
            row?.querySelector(`input[name="${key}"]`) || qs(`input[name="${key}"]`);

        group.addEventListener("click", (e) => {
            const btn = e.target.closest(".chip");
            if (!btn) return;

            const value = btn.dataset.value;

            // ✅ 단일 선택(토글 지원)
            // - 같은 칩을 다시 누르면 해제(=미선택)
            // - '전체' 칩이 있으면 전체로 복귀, 없으면 전부 해제
            if (mode === "single") {
                const isActiveNow = btn.classList.contains("active");
                const allBtn = qs('.chip[data-value=""]', group); // 있으면 "전체"로 복귀 가능

                if (isActiveNow) {
                    // (1) 다시 클릭 = 해제
                    qsa(".chip", group).forEach((c) => c.classList.remove("active"));

                    if (allBtn) {
                        allBtn.classList.add("active");
                        if (singleHidden) singleHidden.value = "";
                    } else {
                        // 작성 페이지처럼 '전체'가 없으면 그냥 미선택 상태로
                        if (singleHidden) singleHidden.value = "";
                    }

                    renderMiniSummary();
                    return;
                }

                // (2) 일반 클릭 = 해당 칩 선택
                qsa(".chip", group).forEach((c) => c.classList.remove("active"));
                btn.classList.add("active");

                if (singleHidden) singleHidden.value = value;

                renderMiniSummary();
                return;
            }

            // ✅ 다중 선택 (theme 같은 multi-or)
            if (mode === "multi-or" && key === "theme") {
                const wrap = qs("#themeInputs") || postForm;

                // 전체 클릭(옵션): data-value=""을 쓰는 경우
                if (value === "") {
                    qsa(".chip", group).forEach((c) => c.classList.remove("active"));
                    btn.classList.add("active");
                    qsa('input[name="themes"]', wrap).forEach((el) => el.remove());
                    renderMiniSummary();
                    return;
                }

                // 전체 버튼이 있다면 비활성화
                const allBtn = qs('.chip[data-value=""]', group);
                if (allBtn) allBtn.classList.remove("active");

                // 토글
                btn.classList.toggle("active");

                // 기존 themes hidden 제거 후 재생성
                qsa('input[name="themes"]', wrap).forEach((el) => el.remove());

                const selected = qsa(".chip.active", group)
                    .map((c) => (c.dataset.value || "").trim())
                    .filter((v) => v && v !== "");

                // 아무것도 없으면 전체 활성(있을 때만)
                if (selected.length === 0) {
                    qsa(".chip", group).forEach((c) => c.classList.remove("active"));
                    if (allBtn) allBtn.classList.add("active");
                    renderMiniSummary();
                    return;
                }

                selected.forEach((v) => {
                    const input = document.createElement("input");
                    input.type = "hidden";
                    input.name = "themes";
                    input.value = v;
                    wrap.appendChild(input);
                });

                renderMiniSummary();
                return;
            }

            // ✅ 다중(region) - region-sub에서만 동작
            if (mode === "multi") {
                btn.classList.toggle("active");

                // ✅ hidden inputs는 #regionInputs에만 넣자(작성 페이지 기준으로 가장 안전)
                const wrap =
                    qs("#regionInputs") ||
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

// ✅ 초기 로드: themes hidden -> 테마 칩 active 복원
    (function restoreThemesFromHidden() {
        const group = qs('.filter-chips[data-key="theme"]');
        if (!group) return;

        const selected = qsa('#themeInputs input[name="themes"]')
            .map((i) => (i.value || "").trim())
            .filter((v) => v);

        // 초기화
        qsa(".chip", group).forEach((c) => c.classList.remove("active"));

        const allBtn = qs('.chip[data-value=""]', group);

        if (selected.length === 0) {
            if (allBtn) allBtn.classList.add("active");
            return;
        }

        if (allBtn) allBtn.classList.remove("active");

        selected.forEach((v) => {
            const btn = qs(`.chip[data-value="${v}"]`, group);
            if (btn) btn.classList.add("active");
        });
    })();

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