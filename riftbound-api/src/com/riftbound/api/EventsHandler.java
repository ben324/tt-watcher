package com.riftbound.api;

import com.riftbound.events.Event;
import com.riftbound.events.EventQuery;
import com.riftbound.events.EventSource;
import com.riftbound.events.EventSourceException;
import com.riftbound.events.EventSources;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class EventsHandler implements HttpHandler {
    private final App app;
    EventsHandler(App app) { this.app = app; }
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Http.preflight(ex)) return;
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { Http.methodNotAllowed(ex, "GET, OPTIONS"); return; }
        Optional<User> user = app.requireUser(ex);
        if (user.isEmpty()) return;
        try {
            Map<String, String> q = Http.query(ex.getRequestURI());
            EventQuery query = parseQuery(q);
            EventSource source = EventSources.fromId(Http.first(q, "source", "sourceId"));
            List<Event> fetched = source.list(query);
            List<Event> matched = filter(fetched, q);
            Http.json(ex, 200, encode(source, query, fetched.size(), matched));
        } catch (IllegalArgumentException e) {
            Http.json(ex, 400, Json.obj("error", Json.str("bad_request"), "message", Json.str(e.getMessage())));
        } catch (EventSourceException e) {
            Http.json(ex, statusFor(e), Json.obj("error", Json.str(e.httpStatus == 429 ? "rate_limited" : "source_error"), "source", Json.str(e.sourceId), "httpStatus", Json.num(e.httpStatus), "message", Json.str(e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            Http.json(ex, 500, Json.obj("error", Json.str("internal"), "message", Json.str("List events failed")));
        }
    }
    static EventQuery parseQuery(Map<String, String> q) {
        Double lat = Http.d(q, "lat", "latitude");
        Double lng = Http.d(q, "lng", "lon", "long", "longitude");
        Integer miles = Http.i(q, "radiusmiles", "radius", "miles", "num_miles");
        if (lat == null || lng == null) throw new IllegalArgumentException("lat and lng are required");
        if (miles == null) miles = 25;
        Instant after = null;
        String rawAfter = Http.first(q, "startDateAfter", "start_date_after");
        if (rawAfter != null) {
            try { after = Instant.parse(rawAfter); }
            catch (DateTimeParseException e) { throw new IllegalArgumentException("startDateAfter must be ISO-8601 instant, e.g. 2026-09-10T00:00:00Z"); }
        }
        Integer pageSize = Http.i(q, "pagesize", "page_size");
        Integer maxPages = Http.i(q, "maxpages", "max_pages");
        return new EventQuery(lat, lng, miles, after, pageSize == null ? 25 : pageSize, maxPages == null ? 5 : maxPages);
    }
    static List<Event> filter(List<Event> events, Map<String, String> q) {
        String eventType = Http.first(q, "eventType", "event_type");
        String category = Http.first(q, "categoryContains", "category");
        String city = Http.first(q, "city");
        Integer maxCost = Http.i(q, "maxCostCents", "max_cost_cents");
        Integer minSeats = Http.i(q, "minSeatsLeft", "min_seats_left");
        boolean fullOnly = isTrue(Http.first(q, "full", "fullOnly"));
        List<Event> out = new ArrayList<>();
        for (Event e : events) {
            if (category != null && !(e.name + "\n" + e.categoryHint).toLowerCase(Locale.ROOT).contains(category.toLowerCase(Locale.ROOT))) continue;
            if (city != null && !(e.city + " " + e.address).toLowerCase(Locale.ROOT).contains(city.toLowerCase(Locale.ROOT))) continue;
            if (maxCost != null && e.costCents > maxCost) continue;
            if (minSeats != null) { int left = e.seatsLeft(); if (left >= 0 && left < minSeats) continue; }
            if (fullOnly) { int left = e.seatsLeft(); if (e.capacity <= 0 || left != 0) continue; }
            if (eventType != null && !EventCatalog.matches(e, eventType)) continue;
            if (!Dates.inRange(e.startDatetime, Http.first(q, "startDateAfter", "from"), Http.first(q, "startDateBefore", "to"))) continue;
            out.add(e);
        }
        return out;
    }
    static String encode(EventSource source, EventQuery query, int fetched, List<Event> events) {
        List<String> items = new ArrayList<>(events.size());
        for (Event e : events) items.add(encodeEvent(e));
        return Json.obj("source", Json.str(source.id()), "query", Json.obj("latitude", Json.num(query.latitude), "longitude", Json.num(query.longitude), "radiusMiles", Json.num(query.radiusMiles), "startDateAfter", Json.str(query.startDateAfter.toString()), "pageSize", Json.num(query.pageSize), "maxPages", Json.num(query.maxPages)), "fetched", Json.num(fetched), "count", Json.num(events.size()), "events", Json.arr(items));
    }
    static String encodeEvent(Event e) {
        return Json.obj("id", Json.str(e.id), "name", Json.str(e.name), "store", Json.str(e.store), "city", Json.str(e.city), "address", Json.str(e.address), "startDatetime", Json.str(e.startDatetime), "eventType", Json.str(e.eventType), "status", Json.str(e.status), "categoryHint", Json.str(e.categoryHint), "registered", Json.num(e.registered), "capacity", Json.num(e.capacity), "seatsLeft", Json.num(e.seatsLeft()), "costCents", Json.num(e.costCents), "currency", Json.str(e.currency), "detailUrl", Json.str(e.detailUrl));
    }
    private static int statusFor(EventSourceException e) {
        if (e.httpStatus == 429) return 429;
        if (e.httpStatus >= 400 && e.httpStatus < 600) return 502;
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("not wired") || msg.contains("not available")) return 503;
        return 502;
    }
    private static boolean isTrue(String raw) {
        if (raw == null) return false;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return "1".equals(v) || "true".equals(v) || "yes".equals(v);
    }
}
