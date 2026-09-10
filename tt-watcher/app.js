const API = "";
const tokenKey = "tt-watcher-token";
const $ = (id) => document.getElementById(id);
function token() { return localStorage.getItem(tokenKey) || ""; }
async function api(path, opts = {}) {
  const headers = Object.assign({ Accept: "application/json" }, opts.headers || {});
  if (opts.body && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
  const t = token();
  if (t) headers.Authorization = "Bearer " + t;
  const res = await fetch(API + path, Object.assign({}, opts, { headers }));
  const text = await res.text();
  let data = {};
  try { data = text ? JSON.parse(text) : {}; } catch { data = { message: text }; }
  if (!res.ok) throw new Error(data.message || data.error || res.statusText);
  return data;
}
function setLoggedIn(user) {
  $("auth").classList.add("hidden");
  $("desk").classList.remove("hidden");
  $("who").classList.remove("hidden");
  $("who").innerHTML = (user && user.email ? user.email : "signed in") + ' <button type="button" class="ghost" id="logout">Log out</button>';
  $("logout").onclick = logout;
}
function setLoggedOut() {
  localStorage.removeItem(tokenKey);
  $("auth").classList.remove("hidden");
  $("desk").classList.add("hidden");
  $("who").classList.add("hidden");
  $("who").innerHTML = "";
}
async function logout() { try { await api("/api/auth/logout", { method: "POST" }); } catch (_) {} setLoggedOut(); }
let registerMode = false;
$("tab-login").onclick = () => { registerMode = false; $("tab-login").classList.add("on"); $("tab-register").classList.remove("on"); $("auth-name-wrap").classList.add("hidden"); $("auth-submit").textContent = "Log in"; };
$("tab-register").onclick = () => { registerMode = true; $("tab-register").classList.add("on"); $("tab-login").classList.remove("on"); $("auth-name-wrap").classList.remove("hidden"); $("auth-submit").textContent = "Register"; };
$("auth-form").onsubmit = async (e) => {
  e.preventDefault();
  $("auth-error").textContent = "";
  const payload = { email: $("auth-email").value.trim(), password: $("auth-password").value };
  const name = $("auth-name").value.trim();
  if (registerMode && name) payload.displayName = name;
  try {
    const path = registerMode ? "/api/auth/register" : "/api/auth/login";
    const data = await api(path, { method: "POST", body: JSON.stringify(payload) });
    localStorage.setItem(tokenKey, data.token);
    setLoggedIn(data.user);
    await bootDesk();
  } catch (err) { $("auth-error").textContent = err.message; }
};
$("s-radius").oninput = () => { $("s-radius-val").textContent = $("s-radius").value; };
$("geo").onclick = () => {
  if (!navigator.geolocation) { $("search-error").textContent = "Geolocation is not available"; return; }
  navigator.geolocation.getCurrentPosition((pos) => { $("s-lat").value = pos.coords.latitude.toFixed(5); $("s-lng").value = pos.coords.longitude.toFixed(5); }, () => { $("search-error").textContent = "Could not read location"; });
};
$("search-form").onsubmit = async (e) => {
  e.preventDefault();
  $("search-error").textContent = "";
  const body = { kind: "SEARCH", name: $("s-name").value.trim() || "search", latitude: Number($("s-lat").value), longitude: Number($("s-lng").value), radiusMiles: Number($("s-radius").value || 25), eventType: $("s-type").value || "ALL" };
  if ($("s-from").value) body.startDateAfter = $("s-from").value;
  if ($("s-to").value) body.startDateBefore = $("s-to").value;
  try { await api("/api/watches", { method: "POST", body: JSON.stringify(body) }); await loadWatches(); }
  catch (err) { $("search-error").textContent = err.message; }
};
$("event-form").onsubmit = async (e) => {
  e.preventDefault();
  $("event-error").textContent = "";
  const eventId = $("e-id").value.trim();
  try {
    await api("/api/watches", { method: "POST", body: JSON.stringify({ kind: "EVENT", eventId, name: $("e-name").value.trim() || eventId }) });
    $("e-id").value = "";
    await loadWatches();
  } catch (err) { $("event-error").textContent = err.message; }
};
$("refresh").onclick = () => loadWatches();
function escapeHtml(s) { return String(s == null ? "" : s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;"); }
async function loadWatches() {
  const box = $("watches");
  $("list-error").textContent = "";
  try {
    const data = await api("/api/watches");
    const watches = data.watches || [];
    if (!watches.length) { box.innerHTML = "<li class='meta'>No watches yet.</li>"; return; }
    box.innerHTML = watches.map((w) => {
      const detail = w.kind === "EVENT" ? "event " + (w.eventId || "") : [w.latitude, w.longitude].join(", ") + " · " + w.radiusMiles + "mi · " + (w.eventType || "ALL");
      const dates = [w.startDateAfter, w.startDateBefore].filter(Boolean).join(" → ");
      return "<li><div><span class='badge'>" + escapeHtml(w.kind) + "</span><strong>" + escapeHtml(w.name || w.kind) + "</strong><p class='meta'>" + escapeHtml(detail) + (dates ? " · " + escapeHtml(dates) : "") + "</p></div><button type='button' class='ghost danger' data-del='" + escapeHtml(w.id) + "'>Remove</button></li>";
    }).join("");
    box.querySelectorAll("[data-del]").forEach((btn) => {
      btn.onclick = async () => { await api("/api/watches/" + btn.getAttribute("data-del"), { method: "DELETE" }); await loadWatches(); };
    });
  } catch (err) { $("list-error").textContent = err.message; }
}
async function loadOptions() {
  const sel = $("s-type");
  sel.innerHTML = "";
  try {
    const data = await api("/api/options");
    (data.eventTypes || []).forEach((t) => { const opt = document.createElement("option"); opt.value = t.id; opt.textContent = t.label; sel.appendChild(opt); });
  } catch (_) { sel.innerHTML = '<option value="ALL">Any type</option>'; }
}
async function bootDesk() { await loadOptions(); await loadWatches(); }
async function start() {
  if (!token()) { setLoggedOut(); return; }
  try { const me = await api("/api/auth/me"); setLoggedIn(me.user); await bootDesk(); } catch (_) { setLoggedOut(); }
}
start();
