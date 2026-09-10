const API = "";
const tokenKey = "tt-watcher-token";
const $ = (id) => document.getElementById(id);
const val = (id) => { const el = $(id); return el && "value" in el ? String(el.value) : ""; };
function token() { return localStorage.getItem(tokenKey) || ""; }
async function api(path, opts = {}) {
  const headers = Object.assign({ Accept: "application/json" }, opts.headers || {});
  if (opts.body && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
  const t = token();
  if (t) headers.Authorization = "Bearer " + t;
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), 15000);
  let res;
  try {
    res = await fetch(API + path, Object.assign({}, opts, { headers, signal: ctrl.signal }));
  } catch (err) {
    if (err && err.name === "AbortError") throw new Error("Server did not respond. Is riftbound-api running?");
    throw new Error("Cannot reach the API. Start run-local.bat and open http://127.0.0.1:8080/");
  } finally { clearTimeout(timer); }
  const text = await res.text();
  let data = {};
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
  localStorage.removeItem(tokenKey);
  $("auth").classList.remove("hidden"); $("desk").classList.add("hidden"); $("who").classList.add("hidden"); $("who").innerHTML = "";
}
async function logout() { try { await api("/api/auth/logout", { method: "POST" }); } catch (_) {} setLoggedOut(); }
let registerMode = false;
function setAuthMode(register) {
  registerMode = register;
  $("tab-login").classList.toggle("on", !register);
  $("tab-register").classList.toggle("on", register);
  $("auth-submit").textContent = register ? "Register" : "Log in";
  if ($("auth-password")) $("auth-password").autocomplete = register ? "new-password" : "current-password";
}
if ($("tab-login")) $("tab-login").onclick = () => setAuthMode(false);
if ($("tab-register")) $("tab-register").onclick = () => setAuthMode(true);
if ($("auth-form")) $("auth-form").onsubmit = async (e) => {
  e.preventDefault(); $("auth-error").textContent = "";
  const email = val("auth-email").trim(); const password = val("auth-password");
  if (!email || !password) { $("auth-error").textContent = "Email and password are required"; return; }
  if (registerMode && password.length < 8) { $("auth-error").textContent = "Password must be at least 8 characters"; return; }
  $("auth-submit").disabled = true;
  try {
    const path = registerMode ? "/api/auth/register" : "/api/auth/login";
    const data = await api(path, { method: "POST", body: JSON.stringify({ email, password }) });
    if (!data.token || !data.user) throw new Error("Login did not return a session");
    localStorage.setItem(tokenKey, data.token); setLoggedIn(data.user); await bootDesk();
  } catch (err) { $("auth-error").textContent = err.message; }
  finally { $("auth-submit").disabled = false; }
};
let map, mapMarker, mapCircle;
function currentRadiusMiles() { return Number(val("s-radius") || 25); }
function setPoint(lat, lng, address) {
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;
  if ($("s-lat")) $("s-lat").value = lat.toFixed(5);
  if ($("s-lng")) $("s-lng").value = lng.toFixed(5);
  if ($("s-coords")) $("s-coords").textContent = lat.toFixed(5) + ", " + lng.toFixed(5) + "  ·  " + currentRadiusMiles() + " mi";
  if (address && $("s-address")) $("s-address").value = address;
  if (!map || typeof L === "undefined") return;
  const here = [lat, lng];
  if (!mapMarker) mapMarker = L.marker(here).addTo(map); else mapMarker.setLatLng(here);
  const meters = currentRadiusMiles() * 1609.34;
  if (!mapCircle) mapCircle = L.circle(here, { radius: meters, color: "#d4a54a", weight: 1, fillOpacity: 0.08 }).addTo(map);
  else { mapCircle.setLatLng(here); mapCircle.setRadius(meters); }
  map.setView(here, Math.max(map.getZoom(), 10));
}
function loadScript(src) {
  return new Promise((resolve, reject) => {
    const s = document.createElement("script"); s.src = src; s.async = true;
    s.onload = () => resolve(); s.onerror = () => reject(new Error("Could not load " + src));
    document.head.appendChild(s);
  });
}
function loadCss(href) {
  const link = document.createElement("link"); link.rel = "stylesheet"; link.href = href; document.head.appendChild(link);
}
async function loadLeaflet() {
  if (typeof L !== "undefined") return;
  loadCss("https://unpkg.com/leaflet@1.9.4/dist/leaflet.css");
  try { await loadScript("https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"); }
  catch (_) {
    loadCss("https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css");
    await loadScript("https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js");
  }
}
async function initMap() {
  if (map || !$("map")) return;
  try { await loadLeaflet(); } catch (_) {
    if ($("search-error")) $("search-error").textContent = "Map failed to load. Type an address instead.";
    return;
  }
  if (typeof L === "undefined") return;
  map = L.map("map", { scrollWheelZoom: true }).setView([39.8283, -98.5795], 4);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", { maxZoom: 19, attribution: "&copy; OpenStreetMap" }).addTo(map);
  map.on("click", async (ev) => {
    if ($("search-error")) $("search-error").textContent = "";
    setPoint(ev.latlng.lat, ev.latlng.lng);
    try { const name = await reverseGeocode(ev.latlng.lat, ev.latlng.lng); if (name && $("s-address")) $("s-address").value = name; } catch (_) {}
  });
  setTimeout(() => map && map.invalidateSize(), 80);
}
async function nominatim(url) {
  const res = await fetch(url, { headers: { Accept: "application/json" } });
  if (!res.ok) throw new Error("Could not look up that address");
  return res.json();
}
async function geocodeAddress(q) {
  const hits = await nominatim("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" + encodeURIComponent(q));
  if (!hits || !hits.length) throw new Error("No match for that address");
  return { lat: Number(hits[0].lat), lng: Number(hits[0].lon), label: hits[0].display_name };
}
async function reverseGeocode(lat, lng) {
  const hit = await nominatim("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=" + encodeURIComponent(lat) + "&lon=" + encodeURIComponent(lng));
  return hit && hit.display_name ? hit.display_name : "";
}
if ($("s-radius")) $("s-radius").oninput = () => {
  if ($("s-radius-val")) $("s-radius-val").textContent = val("s-radius") || "25";
  const lat = Number(val("s-lat")); const lng = Number(val("s-lng"));
  if (Number.isFinite(lat) && Number.isFinite(lng)) setPoint(lat, lng);
};
if ($("geocode")) $("geocode").onclick = async () => {
  if ($("search-error")) $("search-error").textContent = "";
  const q = val("s-address").trim();
  if (!q) { if ($("search-error")) $("search-error").textContent = "Type an address first"; return; }
  try { const hit = await geocodeAddress(q); setPoint(hit.lat, hit.lng, hit.label); }
  catch (err) { if ($("search-error")) $("search-error").textContent = err.message; }
};
if ($("s-address")) $("s-address").addEventListener("keydown", (e) => { if (e.key === "Enter") { e.preventDefault(); if ($("geocode")) $("geocode").click(); } });
if ($("geo")) $("geo").onclick = () => {
  if ($("search-error")) $("search-error").textContent = "";
  if (!navigator.geolocation) { $("search-error").textContent = "Geolocation is not available"; return; }
  navigator.geolocation.getCurrentPosition(async (pos) => {
    setPoint(pos.coords.latitude, pos.coords.longitude);
    try { const name = await reverseGeocode(pos.coords.latitude, pos.coords.longitude); if (name && $("s-address")) $("s-address").value = name; } catch (_) {}
  }, () => { $("search-error").textContent = "Could not read location"; });
};
if ($("search-form")) $("search-form").onsubmit = async (e) => {
  e.preventDefault(); if ($("search-error")) $("search-error").textContent = "";
  const latitude = Number(val("s-lat")); const longitude = Number(val("s-lng"));
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude) || !val("s-lat") || !val("s-lng")) {
    $("search-error").textContent = "Pick a point on the map or look up an address"; return;
  }
  const body = { kind: "SEARCH", name: (val("s-type") || "ALL") + " " + (val("s-radius") || 25) + "mi", latitude, longitude, radiusMiles: Number(val("s-radius") || 25), eventType: val("s-type") || "ALL" };
  if (val("s-from")) body.startDateAfter = val("s-from");
  if (val("s-to")) body.startDateBefore = val("s-to");
  try { await api("/api/watches", { method: "POST", body: JSON.stringify(body) }); await loadWatches(); }
  catch (err) { $("search-error").textContent = err.message; }
};
if ($("event-form")) $("event-form").onsubmit = async (e) => {
  e.preventDefault(); if ($("event-error")) $("event-error").textContent = "";
  const eventId = val("e-id").trim();
  if (!eventId) { $("event-error").textContent = "Event id is required"; return; }
  try {
    await api("/api/watches", { method: "POST", body: JSON.stringify({ kind: "EVENT", eventId, name: eventId }) });
    if ($("e-id")) $("e-id").value = "";
    await loadWatches();
  } catch (err) { $("event-error").textContent = err.message; }
};
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
      const detail = w.kind === "EVENT" ? "event " + (w.eventId || "") : [w.latitude, w.longitude].join(", ") + " · " + w.radiusMiles + "mi · " + (w.eventType || "ALL");
      const dates = [w.startDateAfter, w.startDateBefore].filter(Boolean).join(" → ");
      return "<li><div><span class='badge'>" + escapeHtml(w.kind) + "</span><strong>" + escapeHtml(w.name || w.kind) + "</strong><p class='meta'>" + escapeHtml(detail) + (dates ? " · " + escapeHtml(dates) : "") + "</p></div><button type='button' class='ghost danger' data-del='" + escapeHtml(w.id) + "'>Remove</button></li>";
    }).join("");
    box.querySelectorAll("[data-del]").forEach((btn) => {
      btn.onclick = async () => { await api("/api/watches/" + btn.getAttribute("data-del"), { method: "DELETE" }); await loadWatches(); };
    });
  } catch (err) { if ($("list-error")) $("list-error").textContent = err.message; }
}
async function loadOptions() {
  const sel = $("s-type"); if (!sel) return; sel.innerHTML = "";
  try {
    const data = await api("/api/options");
    (data.eventTypes || []).forEach((t) => { const opt = document.createElement("option"); opt.value = t.id; opt.textContent = t.label; sel.appendChild(opt); });
  } catch (_) { sel.innerHTML = '<option value="ALL">Any type</option>'; }
}
async function bootDesk() { await loadOptions(); await loadWatches(); await initMap(); }
async function start() {
  if (!token()) { setLoggedOut(); return; }
  try { const me = await api("/api/auth/me"); setLoggedIn(me.user); await bootDesk(); }
  catch (_) { setLoggedOut(); }
}
start();
