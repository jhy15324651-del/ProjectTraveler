// info.js (FINAL / copy-paste)
// ✅ contentHtml ONLY (blocks 완전 제거)
// ✅ 기능
// 1) 탭 패널 유지
// 2) 좌측 아코디언
// 3) 관리자 카드 편집 모드
//    - 제목/요약(contenteditable)
//    - 카드 추가(POST /api/admin/info/posts)
//    - 카드 삭제(DELETE /api/admin/info/posts/{postKey})
//    - 위/아래 이동(드래그 없이 화살표)
//    - 저장 시:
//        a) 각 카드 PUT(upsert: title/summary만 갱신)
//        b) reorder PUT(/api/admin/info/posts/reorder?regionKey=..&tabType=..)
// 4) 카드 클릭 -> 인라인 상세(Quill)
//    - 일반: contentHtml 렌더
//    - 관리자: 제목/요약 + Quill 편집/저장/취소
//    - 이미지 버튼 = 서버 업로드(/api/admin/info/upload-image)
//
// ⚠️ 중요: postKey 필드명은 프로젝트 DTO에 따라 다를 수 있음.
// - 카드 DOM은 data-post="POST_KEY" 로 들어와야 함. (th:attr="data-post=${c.postKey}" 또는 ${c.postKey})
// - fetchInfoPost 응답 DTO에 postKey/key 둘 중 하나가 올 수 있으니 안전하게 처리함.

document.addEventListener("DOMContentLoaded", () => {
    /* ==========================
       1) Tab panel safety
    ========================== */
    const activeTab = document.querySelector(".info-tab.is-active");
    const tabKey = activeTab?.dataset?.tab;
    if (tabKey) {
        document.querySelectorAll(".info-panel").forEach((panel) => {
            panel.classList.toggle("is-open", panel.dataset.panel === tabKey);
        });
    }

    /* ==========================
       2) Accordion toggle
    ========================== */
    const accItems = Array.from(document.querySelectorAll(".info-acc__item"));
    accItems.forEach((item) => {
        const head = item.querySelector(".info-acc__head");
        if (!head) return;
        head.addEventListener("click", () => {
            const isOpen = item.classList.contains("is-open");
            accItems.forEach((i) => i.classList.remove("is-open"));
            if (!isOpen) item.classList.add("is-open");
        });
    });

    /* ==========================
       3) Admin edit mode (cards)
    ========================== */
    const editButtons = document.querySelectorAll(".info-editBtn");
    editButtons.forEach((btn) => bindEditButton(btn));

    // 편집 중 링크 이동 방지(탭/사이드바)
    document.addEventListener("click", (e) => {
        const editingPanel = document.querySelector(".info-panel.is-editing");
        if (!editingPanel) return;

        const link = e.target.closest("a");
        if (!link) return;

        if (link.classList.contains("info-tab") || link.classList.contains("info-sidebar__item")) {
            e.preventDefault();
        }
    });

    function bindEditButton(btn) {
        btn.addEventListener("click", async () => {
            const panel = btn.closest(".info-panel");
            if (!panel) return;

            const isEditing = panel.classList.contains("is-editing");
            if (!isEditing) {
                enterEditMode(panel, btn);
                return;
            }

            // ✅ 저장(업서트 + 정렬)
            try {
                await savePanelCards(panel);
                await savePanelOrder(panel);
                alert("저장 완료 ✅");
                exitEditMode(panel, btn);
            } catch (err) {
                console.error(err);
                alert("저장 실패(콘솔 확인)");
            }
        });
    }

    function enterEditMode(panel, btn) {
        panel.classList.add("is-editing");
        btn.classList.add("is-on");
        btn.textContent = "✅ 완료";

        // rollback용: 패널 전체 HTML 저장 (컨트롤 포함 전)
        if (!panel.dataset.originalHtml) panel.dataset.originalHtml = panel.innerHTML;

        // 취소 버튼
        let cancelBtn = panel.querySelector(".info-cancelBtn");
        if (!cancelBtn) {
            cancelBtn = document.createElement("button");
            cancelBtn.type = "button";
            cancelBtn.className = "info-editBtn info-cancelBtn";
            cancelBtn.textContent = "↩ 취소";
            btn.insertAdjacentElement("afterend", cancelBtn);

            cancelBtn.addEventListener("click", () => {
                rollback(panel);
                const newEditBtn = panel.querySelector(".info-editBtn");
                if (newEditBtn) exitEditMode(panel, newEditBtn);
            });
        }

        // 추가 버튼 (패널 헤더 액션영역이 있다면 거기에 붙이는 게 자연스럽지만, 없으면 편집버튼 옆에 삽입)
        let addBtn = panel.querySelector(".info-addBtn");
        if (!addBtn) {
            addBtn = document.createElement("button");
            addBtn.type = "button";
            addBtn.className = "info-editBtn info-addBtn";
            addBtn.textContent = "➕ 추가";
            cancelBtn.insertAdjacentElement("afterend", addBtn);

            addBtn.addEventListener("click", async (e) => {
                e.preventDefault();
                e.stopPropagation();

                try {
                    const regionKey = getRegionKey();
                    const tabType = mapTabType(panel.dataset.panel);

                    const created = await apiCreatePost({
                        regionKey,
                        tabType,
                        title: "새 항목",
                        summary: "",
                        contentHtml: ""
                    });

                    // create 응답이 {postKey:"..."} 라는 전제 (다르면 아래 normalizePostKey에서 커버)
                    const postKey = normalizePostKey(created);
                    if (!postKey) throw new Error("create api: postKey missing");

                    const grid = panel.querySelector(".info-cardGrid");
                    if (!grid) return;

                    const card = document.createElement("article");
                    card.className = "info-card";
                    card.dataset.post = postKey;
                    card.innerHTML = `
            <h4 class="info-card__name">새 항목</h4>
            <p class="info-card__desc"></p>
          `;
                    grid.appendChild(card);

                    makeCardEditable(card);
                    attachCardControls(card, panel);
                } catch (err) {
                    console.error(err);
                    alert("추가 실패(콘솔 확인)");
                }
            });
        }

        // 기존 카드들 편집 가능 + 컨트롤 부착
        panel.querySelectorAll(".info-card").forEach((card) => {
            makeCardEditable(card);
            attachCardControls(card, panel);
        });

        // 커서
        const first = panel.querySelector(".info-card__name");
        if (first) {
            placeCaretAtEnd(first);
            first.focus();
        }
    }

    function exitEditMode(panel, btn) {
        panel.classList.remove("is-editing");
        if (btn) {
            btn.classList.remove("is-on");
            btn.textContent = "✏️ 편집";
        }

        panel.querySelectorAll('[contenteditable="true"]').forEach((node) => {
            node.removeAttribute("contenteditable");
        });

        panel.querySelectorAll(".info-cardCtrl").forEach((c) => c.remove());

        panel.querySelector(".info-cancelBtn")?.remove();
        panel.querySelector(".info-addBtn")?.remove();
    }

    function rollback(panel) {
        const original = panel.dataset.originalHtml;
        if (!original) return;

        // 인라인 상세가 패널 안에 있었다면 같이 날아갈 수 있음.
        // (현재 구조는 #infoInlineDetail이 panel 내부일 수 있으므로) rollback 전에 닫아줌
        closeInlineDetail(true);

        panel.innerHTML = original;

        // rollback 후 이벤트 재바인딩
        panel.querySelectorAll(".info-editBtn").forEach((btn) => bindEditButton(btn));
    }

    function makeCardEditable(card) {
        card.querySelectorAll(".info-card__name, .info-card__desc").forEach((node) => {
            node.setAttribute("contenteditable", "true");
            node.setAttribute("spellcheck", "false");
        });
    }

    function attachCardControls(card, panel) {
        if (card.querySelector(".info-cardCtrl")) return;

        const ctrl = document.createElement("div");
        ctrl.className = "info-cardCtrl";
        ctrl.innerHTML = `
      <button type="button" class="info-cardCtrl__btn" data-act="up" title="위로">↑</button>
      <button type="button" class="info-cardCtrl__btn" data-act="down" title="아래로">↓</button>
      <button type="button" class="info-cardCtrl__btn" data-act="del" title="삭제">삭제</button>
    `;
        card.appendChild(ctrl);

        ctrl.addEventListener("click", async (e) => {
            e.preventDefault();
            e.stopPropagation();

            const act = e.target.closest("[data-act]")?.dataset?.act;
            if (!act) return;

            if (act === "up") {
                const prev = card.previousElementSibling;
                if (prev && prev.classList.contains("info-card")) {
                    card.parentElement.insertBefore(card, prev);
                }
                return;
            }

            if (act === "down") {
                const next = card.nextElementSibling;
                if (next && next.classList.contains("info-card")) {
                    card.parentElement.insertBefore(next, card);
                }
                return;
            }

            if (act === "del") {
                const postKey = card.dataset.post;
                if (!postKey) return;

                if (!confirm("이 카드를 삭제할까요?")) return;

                try {
                    if (current.card === card) closeInlineDetail();

                    await apiDeletePost(postKey);
                    card.remove();
                } catch (err) {
                    console.error(err);
                    alert("삭제 실패(콘솔 확인)");
                }
            }
        });
    }

    function placeCaretAtEnd(el) {
        const range = document.createRange();
        range.selectNodeContents(el);
        range.collapse(false);
        const sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(range);
    }

    /* ==========================
       4) Inline Detail (Quill)
    ========================== */
    const current = {
        panel: null,
        card: null,
        postKey: null,
        postDto: null,
        editing: false,
        quill: null
    };

    // 카드 클릭 -> 상세 열기 (편집모드 아닐 때만)
    document.addEventListener("click", async (e) => {
        const card = e.target.closest(".info-card[data-post]");
        if (!card) return;

        const panel = card.closest(".info-panel");
        if (!panel) return;

        // 카드 편집중이면 상세 열기 금지
        if (panel.classList.contains("is-editing")) return;

        // 카드 컨트롤 클릭은 무시
        if (e.target.closest(".info-cardCtrl")) return;

        const postKey = card.dataset.post;
        if (!postKey) return;

        try {
            const post = await fetchInfoPost(postKey);
            openInlineDetail(panel, card, post);
        } catch (err) {
            console.error(err);
            alert("상세 데이터를 불러오지 못했습니다(콘솔 확인)");
        }
    });

    // 인라인 버튼들
    document.addEventListener("click", async (e) => {
        if (e.target.closest("#btnClose")) {
            closeInlineDetail();
            return;
        }

        if (e.target.closest("#btnInlineEdit")) {
            if (!isAdminUser()) return;
            if (!current.editing) enterInlineEdit();
            return;
        }

        if (e.target.closest("#btnCancel")) {
            if (!isAdminUser()) return;
            rollbackInlineEdit();
            return;
        }

        if (e.target.closest("#btnSave")) {
            if (!isAdminUser()) return;
            await saveInlineEdit();
            return;
        }
    });

    function openInlineDetail(panel, card, post) {
        if (current.card === card && isInlineOpen()) {
            closeInlineDetail();
            return;
        }

        closeInlineDetail();

        current.panel = panel;
        current.card = card;
        current.postKey = normalizePostKey(post) || card.dataset.post;
        current.postDto = post;
        current.editing = false;

        const inline = document.getElementById("infoInlineDetail");
        if (!inline) return;

        inline.classList.remove("is-open"); // 혹시 남아있을까봐
        card.insertAdjacentElement("afterend", inline);

        // 애니메이션을 확실히 시작시키기 위해 한 프레임 뒤에 open
        requestAnimationFrame(() => inline.classList.add("is-open"));

        card.classList.add("is-open");

        const titleEl = document.getElementById("editTitle");
        const summaryEl = document.getElementById("editSummary");

        if (titleEl) titleEl.textContent = post?.title || "";
        if (summaryEl) summaryEl.textContent = post?.summary || "";

        if (titleEl) titleEl.setAttribute("contenteditable", "false");
        if (summaryEl) summaryEl.setAttribute("contenteditable", "false");

        renderContentView(post);
        syncInlineButtons(false);

        inline.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }

    function closeInlineDetail(skipResetQuill) {
        const inline = document.getElementById("infoInlineDetail");
        if (inline) inline.classList.remove("is-open");

        if (current.panel) {
            current.panel.querySelectorAll(".info-card.is-open").forEach((c) => c.classList.remove("is-open"));
        }

        current.editing = false;
        syncInlineButtons(false);

        if (!skipResetQuill && current.quill) current.quill.setText("");

        current.panel = null;
        current.card = null;
        current.postKey = null;
        current.postDto = null;
    }

    function isInlineOpen() {
        const inline = document.getElementById("infoInlineDetail");
        return inline && inline.classList.contains("is-open");
    }

    function syncInlineButtons(editing) {
        const btnEdit = document.getElementById("btnInlineEdit");
        const btnSave = document.getElementById("btnSave");
        const btnCancel = document.getElementById("btnCancel");

        if (btnEdit) btnEdit.style.display = isAdminUser() ? "" : "none";

        if (!isAdminUser()) {
            if (btnSave) btnSave.style.display = "none";
            if (btnCancel) btnCancel.style.display = "none";
            return;
        }

        if (btnSave) btnSave.style.display = editing ? "" : "none";
        if (btnCancel) btnCancel.style.display = editing ? "" : "none";
    }

    function renderContentView(post) {
        const view = document.getElementById("infoContentView");
        const editorWrap = document.getElementById("infoContentEditor");

        if (view) {
            view.style.display = "";
            view.innerHTML = "";
            view.innerHTML = pickContentHtml(post);
        }
        if (editorWrap) editorWrap.style.display = "none";
    }

    function enterInlineEdit() {
        if (!current.postDto || !current.postKey) return;

        current.editing = true;

        const titleEl = document.getElementById("editTitle");
        const summaryEl = document.getElementById("editSummary");

        if (titleEl) {
            titleEl.setAttribute("contenteditable", "true");
            titleEl.setAttribute("spellcheck", "false");
        }
        if (summaryEl) {
            summaryEl.setAttribute("contenteditable", "true");
            summaryEl.setAttribute("spellcheck", "false");
        }

        const view = document.getElementById("infoContentView");
        const editorWrap = document.getElementById("infoContentEditor");
        if (view) view.style.display = "none";
        if (editorWrap) editorWrap.style.display = "";

        ensureQuill();
        setQuillHtml(pickContentHtml(current.postDto));

        syncInlineButtons(true);
    }

    function rollbackInlineEdit() {
        if (!current.postDto) return;

        current.editing = false;

        const titleEl = document.getElementById("editTitle");
        const summaryEl = document.getElementById("editSummary");

        if (titleEl) {
            titleEl.textContent = current.postDto.title || "";
            titleEl.setAttribute("contenteditable", "false");
        }
        if (summaryEl) {
            summaryEl.textContent = current.postDto.summary || "";
            summaryEl.setAttribute("contenteditable", "false");
        }

        renderContentView(current.postDto);
        syncInlineButtons(false);
    }

    async function saveInlineEdit() {
        if (!current.postKey) return;

        try {
            const regionKey = getRegionKey();
            const tabType = mapTabType(current.panel?.dataset?.panel);

            const title = document.getElementById("editTitle")?.textContent?.trim() || "";
            const summary = document.getElementById("editSummary")?.textContent?.trim() || "";
            const contentHtml = getQuillHtml();

            const payload = { regionKey, tabType, title, summary, contentHtml };
            await savePostToServer(current.postKey, payload);

            // ✅ 카드 목록 UI 즉시 반영
            if (current.card) {
                const nameEl = current.card.querySelector(".info-card__name");
                const descEl = current.card.querySelector(".info-card__desc");
                if (nameEl) nameEl.textContent = title;
                if (descEl) descEl.textContent = summary;
            }

            const fresh = await fetchInfoPost(current.postKey);
            current.postDto = fresh;
            current.editing = false;

            const titleEl = document.getElementById("editTitle");
            const summaryEl = document.getElementById("editSummary");
            if (titleEl) titleEl.setAttribute("contenteditable", "false");
            if (summaryEl) summaryEl.setAttribute("contenteditable", "false");

            renderContentView(fresh);
            syncInlineButtons(false);

            alert("저장 완료 ✅");
        } catch (err) {
            console.error(err);
            alert("저장 실패(콘솔 확인)");
        }
    }

    /* ==========================
       Quill + image upload
    ========================== */
    function ensureQuill() {
        if (current.quill) return current.quill;

        const editorEl = document.getElementById("quillEditor");
        if (!editorEl) return null;

        current.quill = new Quill("#quillEditor", {
            theme: "snow",
            modules: {
                toolbar: [
                    [{ header: [1, 2, 3, false] }],
                    ["bold", "italic", "underline", "strike"],
                    [{ list: "ordered" }, { list: "bullet" }],
                    [{ align: [] }],
                    ["link", "image"],
                    ["clean"]
                ]
            }
        });

        const toolbar = current.quill.getModule("toolbar");
        toolbar.addHandler("image", () => selectLocalImageAndUpload());

        return current.quill;
    }

    function setQuillHtml(html) {
        if (!current.quill) return;
        current.quill.clipboard.dangerouslyPasteHTML(html || "");
    }

    function getQuillHtml() {
        if (!current.quill) return "";
        return current.quill.root?.innerHTML || "";
    }

    function pickContentHtml(post) {
        return String(post?.contentHtml || "").trim();
    }

    function selectLocalImageAndUpload() {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = "image/*";
        input.click();

        input.onchange = async () => {
            const file = input.files ? input.files[0] : null;
            if (!file) return;

            try {
                const url = await uploadInfoImage(file);
                insertImageToQuill(url);
            } catch (e) {
                console.error(e);
                alert("이미지 업로드 실패(콘솔 확인)");
            }
        };
    }

    async function uploadInfoImage(file) {
        const formData = new FormData();
        formData.append("image", file);

        const csrf = getCsrfHeader();
        const headers = {};
        if (csrf) headers[csrf.header] = csrf.token;

        const res = await fetch("/api/admin/info/upload-image", {
            method: "POST",
            headers,
            body: formData
        });

        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(`UPLOAD FAILED ${res.status}: ${text}`);
        }

        const data = await res.json();
        if (!data.url) throw new Error("No url returned");
        return data.url;
    }

    function insertImageToQuill(url) {
        if (!current.quill) return;
        const range = current.quill.getSelection(true);
        const index = range ? range.index : current.quill.getLength();
        current.quill.insertEmbed(index, "image", url, "user");
        current.quill.setSelection(index + 1, 0);
    }

    /* ==========================
       Panel save: upsert + reorder
    ========================== */
    function buildUpsertRequestFromCard(card, regionKey, tabType) {
        const title = card.querySelector(".info-card__name")?.textContent?.trim() || "";
        const summary = card.querySelector(".info-card__desc")?.textContent?.trim() || "";
        return { regionKey, tabType, title, summary }; // ✅ 본문은 건드리지 않음
    }

    async function savePanelCards(panel) {
        const regionKey = getRegionKey();
        const tabType = mapTabType(panel.dataset.panel);

        const cards = Array.from(panel.querySelectorAll(".info-card[data-post]"));
        for (const card of cards) {
            const postKey = card.dataset.post;
            if (!postKey) continue;
            const payload = buildUpsertRequestFromCard(card, regionKey, tabType);
            await savePostToServer(postKey, payload);
        }
    }

    async function savePanelOrder(panel) {
        const regionKey = getRegionKey();
        const tabType = mapTabType(panel.dataset.panel);

        const cards = Array.from(panel.querySelectorAll(".info-card[data-post]"));
        const items = cards
            .map((card, idx) => ({ postKey: card.dataset.post, sortOrder: idx }))
            .filter((x) => !!x.postKey);

        await apiReorder(regionKey, tabType, { items });
    }

    /* ==========================
       API helpers
    ========================== */
    async function fetchInfoPost(postKey) {
        const res = await fetch(`/api/info/posts/${encodeURIComponent(postKey)}`, {
            headers: { Accept: "application/json" }
        });
        if (!res.ok) throw new Error("HTTP " + res.status);
        return await res.json();
    }

    async function savePostToServer(postKey, payload) {
        const csrf = getCsrfHeader();
        const headers = { "Content-Type": "application/json", Accept: "application/json" };
        if (csrf) headers[csrf.header] = csrf.token;

        const res = await fetch(`/api/admin/info/posts/${encodeURIComponent(postKey)}`, {
            method: "PUT",
            headers,
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(`SAVE FAILED ${res.status}: ${text}`);
        }
    }

    async function apiCreatePost(payload) {
        const csrf = getCsrfHeader();
        const headers = { "Content-Type": "application/json", Accept: "application/json" };
        if (csrf) headers[csrf.header] = csrf.token;

        const res = await fetch("/api/admin/info/posts", {
            method: "POST",
            headers,
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(`CREATE FAILED ${res.status}: ${text}`);
        }
        return await res.json();
    }

    async function apiDeletePost(postKey) {
        const csrf = getCsrfHeader();
        const headers = { Accept: "application/json" };
        if (csrf) headers[csrf.header] = csrf.token;

        const res = await fetch(`/api/admin/info/posts/${encodeURIComponent(postKey)}`, {
            method: "DELETE",
            headers
        });

        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(`DELETE FAILED ${res.status}: ${text}`);
        }
    }

    async function apiReorder(regionKey, tabType, body) {
        const csrf = getCsrfHeader();
        const headers = { "Content-Type": "application/json", Accept: "application/json" };
        if (csrf) headers[csrf.header] = csrf.token;

        const url =
            `/api/admin/info/posts/reorder?regionKey=${encodeURIComponent(regionKey)}` +
            `&tabType=${encodeURIComponent(tabType)}`;

        const res = await fetch(url, {
            method: "PUT",
            headers,
            body: JSON.stringify(body)
        });

        if (!res.ok) {
            const text = await res.text().catch(() => "");
            throw new Error(`REORDER FAILED ${res.status}: ${text}`);
        }
    }

    function getCsrfHeader() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
        if (!token || !header) return null;
        return { header, token };
    }

    /* ==========================
       Utils
    ========================== */
    function isAdminUser() {
        const v = document.body?.dataset?.admin;
        if (v === "true") return true;
        if (v === "false") return false;
        return document.querySelectorAll(".info-editBtn").length > 0;
    }

    function getRegionKey() {
        return (
            document.querySelector(".info-mainCard")?.dataset?.region ||
            new URLSearchParams(window.location.search).get("region") ||
            "unknown"
        );
    }

    function mapTabType(panelTabKey) {
        const key = String(panelTabKey || "").toLowerCase();
        if (key === "food") return "FOOD";
        if (key === "spot") return "SPOT";
        if (key === "history") return "HISTORY";
        const upper = String(panelTabKey || "").toUpperCase();
        if (["FOOD", "SPOT", "HISTORY"].includes(upper)) return upper;
        return "FOOD";
    }

    function normalizePostKey(obj) {
        // 응답 DTO가 {postKey}, {key}, {id} 등으로 올 수 있으니 안전장치
        if (!obj) return null;
        return obj.postKey || obj.key || obj.id || null;
    }
});
