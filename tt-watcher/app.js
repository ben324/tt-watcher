const API = "";
const tokenKey = "tt-watcher-token";
const $ = (id) => document.getElementById(id);
const val = (id) => {
  const el = $(id);
  return el && "value" in el ? String(el.value) : "";
};
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
function setAuthMode(register) {
  registerMode = register;
  $("tab-login").classList.toggle("on", !register);
  $("tab-register").classList.toggle("on", register);
  $("auth-submit").textContent = register ? "Register" : "Log in";
  $("auth-password").autocomplete = register ? "new-password" : "current-password";
}
$("tab-login").onclick = () => setAuthMode(false);
$("tab-register").onclick = () => setAuthMode(true);
$("auth-form").onsubmit = async (e) => {
  e.preventDefault();
  $("auth-error").textContent = "";
  const email = val("auth-email").trim();
  const password = val("auth-password");
  if (!email || !password) { $("auth-error").textContent = "Email and password are required"; return; }
  if (registerMode && password.length < 8) { $("auth-error").textContent = "Password must be at least 8 characters"; return; }
  $("auth-submit").disabled = true;
  try {
    const path = registerMode ? "/api/auth/register" : "/api/auth/login";
    const data = await api(path, { method: "POST", body: JSON.stringify({ email, password }) });
    if (!data.token || !data.user) throw new Error("Login did not return a session");
    localStorage.setItem(tokenKey, data.token);
    setLoggedIn(data.user);
    await bootDesk();
  } catch (err) { $("auth-error").textContent = err.message; }
  finally { $("auth-submit").disabled = false; }
};
$("s-radius").oninput = () => { $("s-radius-val").textContent = val("s-radius") || "25"; };
$("geo").onclick = () => {
  if (!navigator.geolocation) { $("search-error").textContent = "Geolocation is not available"; return; }
  navigator.geolocation.getCurrentPosition((pos) => { $("s-lat").value = pos.coords.latitude.toFixed(5); $("s-lng").value = pos.coords.longitude.toFixed(5); }, () => { $("search-error").textContent = "Could not read location"; });
};
$("search-form").onsubmit = async (e) => {
  e.preventDefault();
  $("search-error").textContent = "";
  const body = {
    kind: "SEARCH",
    name: (val("s-type") || "ALL") + " " + (val("s-radius") || 25) + "mi",
    latitude: Number(val("s-lat")),
    longitude: Number(val("s-lng")),
    radiusMiles: Number(val("s-radius") || 25),
    eventType: val("s-type") || "ALL"
  };
  if (val("s-from")) body.startDateAfter = val("s-from");
  if (val("s-to")) body.startDateBefore = val("s-to");
  try { await api("/api/watches", { method: "POST", body: JSON.stringify(body) }); await loadWatches(); }
  catch (err) { $("search-error").textContent = err.message; }
};
$("event-form").onsubmit = async (e) => {
  e.preventDefault();
  $("event-error").textContent = "";
  const eventId = val("e-id").trim();
  if (!eventId) { $("event-error").textContent = "Event id is required"; return; }
  try {
    await api("/api/watches", { method: "POST", body: JSON.stringify({ kind: "EVENT", eventId, name: eventId }) });
    const idBox = $("e-id");
    if (idBox) idBox.value = "";
    await loadWatches();
  } catch (err) { $("event-error").textContent = err.message; }
};
$("refresh").onclick = () => loadWatches();
function escapeHtml(s) {
  return String(s == null ? "" : s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
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
  if (!sel) return;
  sel.innerHTML = "";
  try {
    const data = await api("/api/options");
    (data.eventTypes || []).forEach((t) => { const opt = document.createElement("option"); opt.value = t.id; opt.textContent = t.label; sel.appendChild(opt); });
  } catch (_) { sel.innerHTML = '<option value="ALL">Any type</option>'; }
}
async function bootDesk() { await loadOptions(); await loadWatches(); }
async function start() {
  if (!token()) { setLoggedOut(); return; }
  try { const me = await api("/api/auth/me"); setLoggedIn(me.user); await bootDesk(); }
  catch (_) { setLoggedOut(); }
}
start();
