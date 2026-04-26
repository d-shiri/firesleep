const $ = (id) => document.getElementById(id);

async function api(method, path, body) {
  const opts = { method, headers: {} };
  if (body !== undefined) {
    opts.headers["Content-Type"] = "application/json";
    opts.body = JSON.stringify(body);
  }
  const r = await fetch(path, opts);
  if (r.status === 204) return null;
  const text = await r.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { /* not json */ }
  if (!r.ok) {
    const msg = (data && data.detail) || text || `HTTP ${r.status}`;
    throw new Error(msg);
  }
  return data;
}

function setStatus(el, message, kind) {
  if (!el) return;
  el.textContent = message || "";
  el.classList.remove("ok", "err");
  if (kind) el.classList.add(kind);
}

function esc(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

const BRAND_HELP = {
  lg: "Direct webOS connection. Pairing prompts on the TV — accept with the LG remote.",
  firetv: "Sends KEYCODE_SLEEP over ADB. The Fire TV goes to standby and pulls the TV down over HDMI-CEC. Enable ADB debugging on the Fire TV first; pairing pops 'Allow USB debugging' there — tick 'Always allow'.",
};

const BRAND_LABEL = {
  lg: "LG webOS",
  firetv: "Fire TV → CEC",
};

function brandSelectHtml(selected) {
  return `
    <select class="tv-brand">
      <option value="lg" ${selected === "lg" ? "selected" : ""}>LG webOS (direct)</option>
      <option value="firetv" ${selected === "firetv" ? "selected" : ""}>Fire TV → CEC (Samsung / Sony / Vizio / Hisense / …)</option>
    </select>`;
}

function tvCardHtml(tv) {
  const dotClass = tv.paired ? "ok" : "warn";
  const pairedClass = tv.paired ? "ok" : "warn";
  const pairedText = tv.paired ? "Paired" : "Not paired";
  const brandLabel = BRAND_LABEL[tv.brand] || tv.brand;
  return `
  <article class="tv-card" data-id="${esc(tv.id)}">
    <div class="tv-row" data-toggle>
      <span class="dot ${dotClass}" title="${pairedText}"></span>
      <div class="info">
        <span class="name">${esc(tv.name)}</span>
        <span class="meta">
          <span class="badge brand">${esc(brandLabel)}</span>
          <span class="host">${esc(tv.host)}</span>
        </span>
      </div>
      <div class="quick">
        <span class="badge ${pairedClass}">${pairedText}</span>
        <button class="ghost small tv-poweroff" title="Power off">Power off</button>
        <span class="chevron">›</span>
      </div>
    </div>
    <div class="tv-detail">
      <div class="row">
        <label>
          <span class="label">Name</span>
          <input class="tv-name" type="text" value="${esc(tv.name)}" />
        </label>
      </div>
      <div class="row">
        <label>
          <span class="label">Backend</span>
          ${brandSelectHtml(tv.brand)}
        </label>
      </div>
      <div class="row">
        <label>
          <span class="label">Host</span>
          <input class="tv-host" type="text" value="${esc(tv.host)}" />
        </label>
      </div>
      <p class="muted small tv-help">${esc(BRAND_HELP[tv.brand] || "")}</p>
      <div class="actions">
        <button class="tv-save primary">Save changes</button>
        <button class="tv-pair ghost">${tv.paired ? "Re-pair" : "Pair"}</button>
        <button class="tv-unpair ghost danger">Forget pairing</button>
        <button class="tv-delete ghost danger" style="margin-left:auto">Remove</button>
        <span class="status tv-status"></span>
      </div>
    </div>
  </article>`;
}

function updateStats(tvs) {
  const total = tvs.length;
  const paired = tvs.filter((t) => t.paired).length;
  $("stat-total").textContent = total;
  $("stat-paired").textContent = paired;
  $("stat-unpaired").textContent = total - paired;
}

async function refreshTvs() {
  const list = $("tv-list");
  try {
    const tvs = await api("GET", "/tvs");
    list.innerHTML = tvs.map(tvCardHtml).join("");
    updateStats(tvs);
  } catch (e) {
    list.innerHTML = `<p class="status err">Couldn't load TVs: ${esc(e.message)}</p>`;
    updateStats([]);
  }
}

async function refreshConfig() {
  try {
    const cfg = await api("GET", "/config");
    $("log-level").value = cfg.log_level || "INFO";
  } catch (e) {
    setStatus($("config-status"), `Couldn't load config: ${e.message}`, "err");
  }
}

function tvFromCard(card) {
  return {
    id: card.dataset.id,
    name: card.querySelector(".tv-name").value.trim(),
    brand: card.querySelector(".tv-brand").value,
    host: card.querySelector(".tv-host").value.trim(),
  };
}

async function withButtons(card, statusEl, busyMsg, fn) {
  const all = card.querySelectorAll("button");
  all.forEach((b) => (b.disabled = true));
  setStatus(statusEl, busyMsg);
  try {
    await fn();
  } catch (e) {
    setStatus(statusEl, e.message, "err");
    throw e;
  } finally {
    all.forEach((b) => (b.disabled = false));
  }
}

$("tv-list").addEventListener("click", async (ev) => {
  const card = ev.target.closest(".tv-card");
  if (!card) return;

  const toggleZone = ev.target.closest("[data-toggle]");
  const btn = ev.target.closest("button");

  // Toggle expand only when clicking the row (not buttons or inputs)
  if (toggleZone && !btn) {
    card.classList.toggle("expanded");
    return;
  }
  if (!btn) return;

  const tv = tvFromCard(card);
  const status = card.querySelector(".tv-status");

  if (btn.classList.contains("tv-poweroff")) {
    if (!confirm(`Power off "${tv.name}" now?`)) return;
    try {
      await withButtons(card, status, "Sending…", async () => {
        await api("POST", `/tvs/${tv.id}/poweroff`);
        setStatus(status, "TV acknowledged ✓", "ok");
      });
    } catch { /* shown */ }
    return;
  }

  if (btn.classList.contains("tv-save")) {
    try {
      await withButtons(card, status, "Saving…", async () => {
        await api("PUT", `/tvs/${tv.id}`, { name: tv.name, brand: tv.brand, host: tv.host });
        setStatus(status, "Saved ✓", "ok");
        await refreshTvs();
      });
    } catch { /* shown */ }
    return;
  }

  if (btn.classList.contains("tv-pair")) {
    try {
      await withButtons(card, status, "Accept the prompt on the TV…", async () => {
        await api("POST", `/tvs/${tv.id}/pair`);
        setStatus(status, "Paired ✓", "ok");
        await refreshTvs();
      });
    } catch { /* shown */ }
    return;
  }

  if (btn.classList.contains("tv-unpair")) {
    if (!confirm(`Forget pairing for "${tv.name}"?`)) return;
    try {
      await withButtons(card, status, "Forgetting…", async () => {
        await api("POST", `/tvs/${tv.id}/unpair`);
        setStatus(status, "Forgotten ✓", "ok");
        await refreshTvs();
      });
    } catch { /* shown */ }
    return;
  }

  if (btn.classList.contains("tv-delete")) {
    if (!confirm(`Remove "${tv.name}" from the bridge? This also forgets its pairing.`)) return;
    try {
      await withButtons(card, status, "Removing…", async () => {
        await api("DELETE", `/tvs/${tv.id}`);
        await refreshTvs();
      });
    } catch { /* shown */ }
    return;
  }
});

$("tv-list").addEventListener("change", (ev) => {
  if (!ev.target.classList.contains("tv-brand")) return;
  const card = ev.target.closest(".tv-card");
  card.querySelector(".tv-help").textContent = BRAND_HELP[ev.target.value] || "";
});

$("save-config").addEventListener("click", async () => {
  const btn = $("save-config");
  btn.disabled = true;
  setStatus($("config-status"), "Saving…");
  try {
    await api("POST", "/config", { log_level: $("log-level").value });
    setStatus($("config-status"), "Saved ✓", "ok");
  } catch (e) {
    setStatus($("config-status"), e.message, "err");
  } finally {
    btn.disabled = false;
  }
});

/* ---- Add-TV modal ---- */

const modal = $("add-modal");

function openAddModal() {
  modal.hidden = false;
  setStatus($("add-status"), "");
  $("new-help").textContent = BRAND_HELP[$("new-brand").value] || "";
  setTimeout(() => $("new-name").focus(), 50);
}

function closeAddModal() {
  modal.hidden = true;
}

$("open-add").addEventListener("click", openAddModal);
$("close-add").addEventListener("click", closeAddModal);
$("cancel-add").addEventListener("click", closeAddModal);

modal.addEventListener("click", (ev) => {
  if (ev.target === modal) closeAddModal();
});

document.addEventListener("keydown", (ev) => {
  if (ev.key === "Escape" && !modal.hidden) closeAddModal();
});

$("new-brand").addEventListener("change", () => {
  $("new-help").textContent = BRAND_HELP[$("new-brand").value] || "";
});

$("add-tv").addEventListener("click", async () => {
  const name = $("new-name").value.trim();
  const brand = $("new-brand").value;
  const host = $("new-host").value.trim();
  if (!name || !host) {
    setStatus($("add-status"), "Name and host are required.", "err");
    return;
  }
  const btn = $("add-tv");
  btn.disabled = true;
  setStatus($("add-status"), "Adding…");
  try {
    await api("POST", "/tvs", { name, brand, host });
    setStatus($("add-status"), "Added ✓", "ok");
    $("new-name").value = "";
    $("new-host").value = "";
    await refreshTvs();
    closeAddModal();
  } catch (e) {
    setStatus($("add-status"), e.message, "err");
  } finally {
    btn.disabled = false;
  }
});

refreshConfig();
refreshTvs();
