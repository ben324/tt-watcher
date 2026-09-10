const API = "";
const tokenKey = "tt-watcher-token";
const $ = (id) => document.getElementById(id);
const val = (id) => { const el = $(id); return el && "value" in el ? String(el.value) : ""; };
function token() { return localStorage.getItem(tokenKey) || ""; }
async function api(path, opts = {}) {
  const headers = Object.assign({ Accept: "application/json" }, opts.headers || {});
  if (opts.body && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
  const t = token(); if (t) headers.Authorization = "Bearer " + t;
  const ctrl = new AbortController(); const timer = setTimeout(() => ctrl.abort(), 15000);
  let res;
  try { res = await fetch(API + path, Object.assign({}, opts, { headers, signal: ctrl.signal })); }
  catch (err) {
    if (err && err.name === "AbortError") throw new Error("Server did not respond. Is riftbound-api running?");
    throw new Error("Cannot reach the API. Start run-local.bat and open http://127.0.0.1:8080/");
  } finally { clearTimeout(timer); }
  const text = await res.text(); let data = {};
  try { data = text ? JSON.parse(text) : {}; } catch { data = { message: text }; }
  if (!res.ok) throw new Error(data.message || data.error || res.statusText);
  return data;
}
function setLoggedIn(user) {
  $("auth").classList.add("hidden"); $("desk").classList.remove("hidden"); $("who").classList.remove("hidden");
  $("who").innerHTML = (user && user.email ? user.email : "signed in") + ' <button type="button" class="ghost" id="logout">Log out</button>';
  $("logout").onclick = logout;
}
function setLoggedOut() {
  $("auth").classList.remove("hidden"); $("desk").classList.add("hidden"); $("who").classList.add("hidden"); $("who").innerHTML = "";
}
async function logout() {
  try { await api("/api/auth/logout", { method: "POST" }); } catch (_) {}
  localStorage.removeItem(tokenKey);
  setLoggedOut();
}
window.afterLogin = async function (user) { setLoggedIn(user); await bootDesk(); };
function setPoint(lat, lng, address) {
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;
  if ($("s-lat")) $("s-lat").value = lat.toFixed(5);
  if ($("s-lng")) $("s-lng").value = lng.toFixed(5);
  if (address && $("s-address")) $("s-address").value = address;
  if (typeof window.ttPin === "function") window.ttPin(lat, lng, address);
}
window.setPoint = setPoint;
if ($("search-form")) $("search-form").onsubmit = (e) => { e.preventDefault(); return false; };
if ($("event-form")) $("event-form").onsubmit = (e) => { e.preventDefault(); return false; };
if ($("auth-form")) $("auth-form").onsubmit = (e) => { e.preventDefault(); return false; };
async function saveSearch() {
  if ($("search-error")) $("search-error").textContent = "";
  const latitude = Number(val("s-lat")); const longitude = Number(val("s-lng"));
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude) || !val("s-lat") || !val("s-lng")) {
    if ($("search-error")) $("search-error").textContent = "Find an address first so we can pin it on the map";
    return;
  }
  const body = { kind: "SEARCH", name: (val("s-type") || "ALL") + " " + (val("s-radius") || 25) + "mi", latitude, longitude, radiusMiles: Number(val("s-radius") || 25), eventType: val("s-type") || "ALL" };
  if (val("s-from")) body.startDateAfter = val("s-from");
  if (val("s-to")) body.startDateBefore = val("s-to");
  try { await api("/api/watches", { method: "POST", body: JSON.stringify(body) }); await loadWatches(); }
  catch (err) { if ($("search-error")) $("search-error").textContent = err.message; }
}
async function saveEvent() {
  if ($("event-error")) $("event-error").textContent = "";
  const eventId = val("e-id").trim();
  if (!eventId) { if ($("event-error")) $("event-error").textContent = "Event id is required"; return; }
  try {
    await api("/api/watches", { method: "POST", body: JSON.stringify({ kind: "EVENT", eventId, name: eventId }) });
    if ($("e-id")) $("e-id").value = "";
    await loadWatches();
  } catch (err) { if ($("event-error")) $("event-error").textContent = err.message; }
}
if ($("search-save")) $("search-save").onclick = (e) => { e.preventDefault(); saveSearch(); };
if ($("event-save")) $("event-save").onclick = (e) => { e.preventDefault(); saveEvent(); };
if ($("refresh")) $("refresh").onclick = () => loadWatches();
function escapeHtml(s) {
  return String(s == null ? "" : s).replace(/&/g, "&").replace(/</g, "<").replace(/>/g, ">").replace(/"/g, """);
}
async function loadWatches() {
  const box = $("watches"); if ($("list-error")) $("list-error").textContent = "";
  if (!box) return;
  try {
    const data = await api("/api/watches"); const watches = data.watches || [];
    if (!watches.length) { box.innerHTML = "<li class='meta'>No watches yet.</li>"; return; }
    box.innerHTML = watches.map((w) => {
      const detail = w.kind === "EVENT" ? "event " + (w.eventId || "") : (w.radiusMiles + "mi · " + (w.eventType || "ALL"));
      const dates = [w.startDateAfter, w.startDateBefore].filter(Boolean).join(" → ");
      return "<li><div><span class='badge'>" + escapeHtml(w.kind) + "</span><strong>" + escapeHtml(w.name || w.kind) + "</strong><p class='meta'>" + escapeHtml(detail) + (dates ? " · " + escapeHtml(dates) : "") + "</p></div><button type='button' class='ghost danger' data-del='" + escapeHtml(w.id) + "'>Remove</button></li>";
    }).join("");
    box.querySelectorAll("[data-del]").forEach((btn) => {
      btn.onclick = async () => { await api("/api/watches/" + btn.getAttribute("data-del"), { method: "DELETE" }); await loadWatches(); };
    });
  } catch (err) { if ($("list-error")) $("list-error").textContent = err.message; }
}
async function loadOptions() {
  const sel = $("s-type");
  if (!sel || sel.options.length > 1) return;
}
async function bootDesk() { await loadOptions(); await loadWatches(); }
async function start() {
  if (!token()) { setLoggedOut(); return; }
  try { const me = await api("/api/auth/me"); setLoggedIn(me.user); await bootDesk(); }
  catch (_) { setLoggedOut(); }
}
start();
