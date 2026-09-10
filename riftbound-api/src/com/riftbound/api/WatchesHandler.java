package com.riftbound.api;

import com.riftbound.events.EventSource;
import com.riftbound.events.EventSources;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

final class WatchesHandler implements HttpHandler {
    private final App app;
    WatchesHandler(App app) { this.app = app; }
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Http.preflight(ex)) return;
        Optional<User> user = app.requireUser(ex);
        if (user.isEmpty()) return;
        String path = Http.path(ex);
        String method = ex.getRequestMethod().toUpperCase(Locale.ROOT);
        String id = watchId(path);
        try {
            if (id == null && "GET".equals(method)) { list(ex, user.get()); return; }
            if (id == null && "POST".equals(method)) { create(ex, user.get()); return; }
            if (id != null && "GET".equals(method)) { one(ex, user.get(), id); return; }
            if (id != null && "PATCH".equals(method)) { update(ex, user.get(), id); return; }
            if (id != null && ("DELETE".equals(method) || "POST".equals(method) && path.endsWith("/unsubscribe"))) { delete(ex, user.get(), id); return; }
            Http.methodNotAllowed(ex, "GET, POST, PATCH, DELETE, OPTIONS");
        } catch (IllegalArgumentException e) {
            Http.json(ex, 400, Json.obj("error", Json.str("bad_request"), "message", Json.str(e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            Http.json(ex, 500, Json.obj("error", Json.str("internal"), "message", Json.str("Watch request failed")));
        }
    }
    private void list(HttpExchange ex, User user) throws IOException {
        List<Watch> watches = app.store.watchesFor(user.id);
        List<String> items = new ArrayList<>(watches.size());
        for (Watch w : watches) items.add(w.publicJson());
        Http.json(ex, 200, Json.obj("count", Json.num(items.size()), "watches", Json.arr(items)));
    }
    private void one(HttpExchange ex, User user, String id) throws IOException {
        Optional<Watch> found = app.store.watchFor(user.id, id);
        if (found.isEmpty()) { Http.json(ex, 404, Json.obj("error", Json.str("not_found"), "message", Json.str("No subscription with that id"))); return; }
        Http.json(ex, 200, Json.obj("watch", found.get().publicJson()));
    }
    private void create(HttpExchange ex, User user) throws IOException {
        Watch watch = parseWatch(user.id, UUID.randomUUID().toString(), Instant.now().toString(), Http.readBody(ex), null);
        if (!approve(ex, watch)) return;
        app.store.addWatch(watch);
        Http.json(ex, 201, Json.obj("watch", watch.publicJson()));
    }
    private void update(HttpExchange ex, User user, String id) throws IOException {
        Optional<Watch> existing = app.store.watchFor(user.id, id);
        if (existing.isEmpty()) { Http.json(ex, 404, Json.obj("error", Json.str("not_found"), "message", Json.str("No subscription with that id"))); return; }
        Watch updated = parseWatch(user.id, id, existing.get().createdAt, Http.readBody(ex), existing.get());
        if (!approve(ex, updated)) return;
        app.store.replaceWatch(user.id, id, updated);
        Http.json(ex, 200, Json.obj("watch", updated.publicJson()));
    }
    private boolean approve(HttpExchange ex, Watch watch) throws IOException {
        try { app.eventWatches.requireUpcoming(watch); return true; }
        catch (EventWatchValidator.Rejected e) {
            Http.json(ex, e.httpStatus, Json.obj("error", Json.str(e.code), "message", Json.str(e.getMessage())));
            return false;
        }
    }
    private void delete(HttpExchange ex, User user, String id) throws IOException {
        if (!app.store.deleteWatch(user.id, id)) { Http.json(ex, 404, Json.obj("error", Json.str("not_found"), "message", Json.str("No subscription with that id"))); return; }
        Http.json(ex, 200, Json.obj("ok", Json.bool(true), "id", Json.str(id), "message", Json.str("Unsubscribed")));
    }
    private static Watch parseWatch(String userId, String id, String createdAt, String body, Watch base) {
        if (body == null || body.isBlank()) {
            if (base == null) throw new IllegalArgumentException("JSON body required");
            return base;
        }
        String eventId = firstString(body, base == null ? null : base.eventId, "eventId");
        String kind = Body.str(body, "kind");
        if (kind == null && base != null) kind = base.kind;
        if (kind == null || kind.isBlank()) kind = (eventId != null && !eventId.isBlank()) ? Watch.KIND_EVENT : Watch.KIND_SEARCH;
        kind = kind.toUpperCase(Locale.ROOT);
        if (!Watch.KIND_SEARCH.equals(kind) && !Watch.KIND_EVENT.equals(kind)) throw new IllegalArgumentException("kind must be SEARCH or EVENT");
        if (Watch.KIND_EVENT.equals(kind) && (eventId == null || eventId.isBlank())) throw new IllegalArgumentException("eventId is required for kind=EVENT");
        Double lat = Body.decimal(body, "latitude"); if (lat == null) lat = Body.decimal(body, "lat");
        Double lng = Body.decimal(body, "longitude"); if (lng == null) lng = Body.decimal(body, "lng"); if (lng == null) lng = Body.decimal(body, "lon");
        Integer miles = Body.integer(body, "radiusMiles"); if (miles == null) miles = Body.integer(body, "radius");
        if (base != null) { if (lat == null) lat = base.latitude; if (lng == null) lng = base.longitude; if (miles == null) miles = base.radiusMiles; }
        if (Watch.KIND_SEARCH.equals(kind) && (lat == null || lng == null)) throw new IllegalArgumentException("latitude and longitude are required for kind=SEARCH");
        if (lat == null) lat = 0.0; if (lng == null) lng = 0.0;
        if (!Double.isFinite(lat) || !Double.isFinite(lng)) throw new IllegalArgumentException("latitude/longitude must be finite");
        if (miles == null) miles = 25;
        if (Watch.KIND_SEARCH.equals(kind) && miles <= 0) throw new IllegalArgumentException("radiusMiles must be > 0");
        if (Watch.KIND_SEARCH.equals(kind) && miles > EventCatalog.MAX_RADIUS_MILES) throw new IllegalArgumentException("radiusMiles must be <= " + EventCatalog.MAX_RADIUS_MILES);
        String source = Body.str(body, "source");
        if (source == null && base != null) source = base.source;
        if (source == null || source.isBlank()) source = EventSources.UVS_HYDRA;
        source = EventSources.fromId(source).id();
        String name = firstString(body, base == null ? null : base.name, "name");
        String eventType = firstString(body, base == null ? null : base.eventType, "eventType");
        if (Watch.KIND_SEARCH.equals(kind)) {
            if (eventType == null || eventType.isBlank()) eventType = EventCatalog.ALL;
            EventCatalog.Type type = EventCatalog.find(eventType);
            if (type == null) throw new IllegalArgumentException("eventType is not a known UVS category");
            eventType = type.id();
        }
        String category = firstString(body, base == null ? null : base.categoryContains, "categoryContains", "category");
        String city = firstString(body, base == null ? null : base.city, "city");
        String startAfter = Dates.normalize(firstString(body, base == null ? null : base.startDateAfter, "startDateAfter", "startAfter", "from"));
        String startBefore = Dates.normalize(firstString(body, base == null ? null : base.startDateBefore, "startDateBefore", "startBefore", "to"));
        if (!startAfter.isBlank() && !startBefore.isBlank() && Dates.parseStart(startAfter).isAfter(Dates.parseEnd(startBefore))) throw new IllegalArgumentException("startDateAfter must be before startDateBefore");
        Integer maxCost = Body.has(body, "maxCostCents") ? Body.integer(body, "maxCostCents") : (base == null ? null : base.maxCostCents);
        Integer minSeats = Body.has(body, "minSeatsLeft") ? Body.integer(body, "minSeatsLeft") : (base == null ? null : base.minSeatsLeft);
        if (minSeats != null && minSeats < 0) throw new IllegalArgumentException("minSeatsLeft must be >= 0");
        if (maxCost != null && maxCost < 0) throw new IllegalArgumentException("maxCostCents must be >= 0");
        return new Watch(id, userId, kind, name, eventId, lat, lng, miles, source, eventType, category, city, maxCost, minSeats, startAfter, startBefore, createdAt);
    }
    private static String firstString(String body, String fallback, String... keys) {
        for (String key : keys) if (Body.has(body, key)) { String v = Body.str(body, key); return v == null ? "" : v; }
        return fallback == null ? "" : fallback;
    }
    private static String watchId(String path) {
        String prefix = "/api/watches";
        if (!path.startsWith(prefix)) return null;
        String rest = path.substring(prefix.length());
        if (rest.startsWith("/")) rest = rest.substring(1);
        if (rest.isEmpty()) return null;
        if (rest.endsWith("/unsubscribe")) {
            rest = rest.substring(0, rest.length() - "/unsubscribe".length());
            if (rest.endsWith("/")) rest = rest.substring(0, rest.length() - 1);
        }
        return rest.isEmpty() ? null : rest;
    }
}
