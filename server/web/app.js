const $ = (id) => document.getElementById(id);

async function api(method, path, body) {
  const opts = { method, headers: {} };
  if (body !== undefined) {
    opts.headers["Content-Type"] = "application/json";
    opts.body = JSON.stringify(body);
  }
  const r = await fetch(path, opts);
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
  el.textContent = message || "";
  el.classList.remove("ok", "err");
  if (kind) el.classList.add(kind);
}

function setBadge(el, paired) {
  el.textContent = paired ? "Paired" : "Not paired";
  el.classList.remove("ok", "warn");
  el.classList.add(paired ? "ok" : "warn");
}

async function refresh() {
  try {
    const cfg = await api("GET", "/config");
    $("tv-host").value = cfg.tv_host || "";
    $("log-level").value = cfg.log_level || "INFO";
    setBadge($("paired-badge"), cfg.paired);
  } catch (e) {
    setStatus($("save-status"), `Couldn't load config: ${e.message}`, "err");
  }
}

$("save").addEventListener("click", async () => {
  const btn = $("save");
  btn.disabled = true;
  setStatus($("save-status"), "Saving…");
  try {
    await api("POST", "/config", {
      tv_host: $("tv-host").value.trim(),
      log_level: $("log-level").value,
    });
    setStatus($("save-status"), "Saved ✓", "ok");
  } catch (e) {
    setStatus($("save-status"), e.message, "err");
  } finally {
    btn.disabled = false;
  }
});

$("pair").addEventListener("click", async () => {
  const btn = $("pair");
  btn.disabled = true;
  setStatus($("pair-status"), "Accept the prompt on your TV…");
  try {
    await api("POST", "/pair");
    setStatus($("pair-status"), "Paired ✓", "ok");
    refresh();
  } catch (e) {
    setStatus($("pair-status"), e.message, "err");
  } finally {
    btn.disabled = false;
  }
});

$("unpair").addEventListener("click", async () => {
  if (!confirm("Forget the LG pairing key? You'll need to re-pair.")) return;
  const btn = $("unpair");
  btn.disabled = true;
  setStatus($("pair-status"), "Forgetting…");
  try {
    await api("POST", "/unpair");
    setStatus($("pair-status"), "Pairing forgotten", "ok");
    refresh();
  } catch (e) {
    setStatus($("pair-status"), e.message, "err");
  } finally {
    btn.disabled = false;
  }
});

$("poweroff").addEventListener("click", async () => {
  if (!confirm("Power off the TV now?")) return;
  const btn = $("poweroff");
  btn.disabled = true;
  setStatus($("poweroff-status"), "Sending…");
  try {
    await api("POST", "/poweroff");
    setStatus($("poweroff-status"), "TV acknowledged ✓", "ok");
  } catch (e) {
    setStatus($("poweroff-status"), e.message, "err");
  } finally {
    btn.disabled = false;
  }
});

refresh();
