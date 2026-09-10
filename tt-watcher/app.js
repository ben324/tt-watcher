const API = "";
const tokenKey = "tt-watcher-token";
const GEO_KEY = "87ad304e5f5a4e04a07fdada78297a92";
const $ = (id) => document.getElementById(id);
const val = (id) => { const el = $(id); return el && "value" in el ? String(el.value) : ""; };
function token() { return localStorage.getItem(tokenKey) || ""; }
function geoapifyKey() { return val("s-geo-key").trim() || localStorage.getItem("tt-watcher-geoapify") || GEO_KEY; }
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
  localStorage.removeItem(tokenKey);
  $("auth").classList.remove("hidden"); $("desk").classList.add("hidden"); $("who").classList.add("hidden"); $("who").innerHTML = "";
}
async function logout() { try { await api("/api/auth/logout", { method: "POST" }); } catch (_) {} setLoggedOut(); }
window.afterLogin = async function (user) { setLoggedIn(user); await bootDesk(); };
let map, mapMarker, mapCircle;
function currentRadiusMiles() { return Number(val("s-radius") || 25); }
function setPoint(lat, lng, address) {
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;
  if ($("s-lat")) $("s-lat").value = lat.toFixed(5);
  if ($("s-lng")) $("s-lng").value = lng.toFixed(5);
  if (address && $("s-address")) $("s-address").value = address;
  if ($("s-coords")) $("s-coords").textContent = (address || "Pinned") + "  ·  " + currentRadiusMiles() + " mi";
  if (!map || typeof L === "undefined") {
    Promise.resolve(initMap()).then(() => { if (map) setPoint(lat, lng, address); });
    return;
  }
  const here = [lat, lng];
  if (!mapMarker) mapMarker = L.marker(here).addTo(map); else mapMarker.setLatLng(here);
  const meters = currentRadiusMiles() * 1609.34;
  if (!mapCircle) mapCircle = L.circle(here, { radius: meters, color: "#d4a54a", weight: 1, fillOpacity: 0.08 }).addTo(map);
  else { mapCircle.setLatLng(here); mapCircle.setRadius(meters); }
  map.setView(here, 14);
}
window.setPoint = setPoint;
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
  catch (_) { loadCss("https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css"); await loadScript("https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js"); }
}
async function initMap() {
  if (map || !$("map")) return;
  try { await loadLeaflet(); } catch (_) { return; }
  if (typeof L === "undefined") return;
  map = L.map("map", { scrollWheelZoom: true }).setView([39.8283, -98.5795], 4);
  L.tileLayer("https://maps.geoapify.com/v1/tile/osm-bright/{z}/{x}/{y}.png?apiKey=" + encodeURIComponent(geoapifyKey()), {
    maxZoom: 20, attribution: "Powered by Geoapify | © OpenStreetMap"
  }).addTo(map);
  map.on("click", async (ev) => {
    setPoint(ev.latlng.lat, ev.latlng.lng);
    try {
      const data = await fetch("https://api.geoapify.com/v1/geocode/reverse?format=json&lat=" + ev.latlng.lat + "&lon=" + ev.latlng.lng + "&apiKey=" + encodeURIComponent(geoapifyKey())).then((r) => r.json());
      const hit = data.results && data.results[0];
      if (hit && hit.formatted) setPoint(ev.latlng.lat, ev.latlng.lng, hit.formatted);
    } catch (_) {}
  });
  setTimeout(() => map && map.invalidateSize(), 80);
}
if ($("s-radius")) $("s-radius").oninput = () => {
  if ($("s-radius-val")) $("s-radius-val").textContent = val("s-radius") || "25";
  const lat = Number(val("s-lat")); const lng = Number(val("s-lng"));
  if (Number.isFinite(lat) && Number.isFinite(lng)) setPoint(lat, lng);
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
