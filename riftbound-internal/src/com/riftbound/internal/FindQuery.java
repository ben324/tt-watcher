package com.riftbound.internal;

import com.riftbound.events.EventQuery;
import com.riftbound.events.EventSource;
import com.riftbound.events.EventSources;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

final class FindQuery {

    final EventSource source;
    final EventQuery query;
    final String eventType;
    final String categoryContains;
    final String city;
    final Integer maxCostCents;
    final Integer minSeatsLeft;

    FindQuery(
            EventSource source,
            EventQuery query,
            String eventType,
            String categoryContains,
            String city,
            Integer maxCostCents,
            Integer minSeatsLeft) {
        this.source = source;
        this.query = query;
        this.eventType = eventType == null ? "" : eventType;
        this.categoryContains = categoryContains == null ? "" : categoryContains;
        this.city = city == null ? "" : city;
        this.maxCostCents = maxCostCents;
        this.minSeatsLeft = minSeatsLeft;
    }

    static FindQuery fromQueryString(Map<String, String> q) {
        Double lat = Http.d(q, "lat", "latitude");
        Double lng = Http.d(q, "lng", "lon", "long", "longitude");
        Integer miles = Http.i(q, "radiusmiles", "radius", "miles", "num_miles");
        Instant after = parseInstant(Http.first(q, "startDateAfter", "start_date_after"));
        Integer pageSize = Http.i(q, "pagesize", "page_size");
        Integer maxPages = Http.i(q, "maxpages", "max_pages");
        return build(
                lat, lng, miles, after, pageSize, maxPages,
                Http.first(q, "source", "sourceId"),
                Http.first(q, "eventType", "event_type"),
                Http.first(q, "categoryContains", "category"),
                Http.first(q, "city"),
                Http.i(q, "maxCostCents", "max_cost_cents"),
                Http.i(q, "minSeatsLeft", "min_seats_left"));
    }

    static FindQuery fromJson(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("JSON body required");
        }
        Double lat = Body.decimal(body, "latitude");
        if (lat == null) lat = Body.decimal(body, "lat");
        Double lng = Body.decimal(body, "longitude");
        if (lng == null) lng = Body.decimal(body, "lng");
        if (lng == null) lng = Body.decimal(body, "lon");
        Integer miles = Body.integer(body, "radiusMiles");
        if (miles == null) miles = Body.integer(body, "radius");
        Instant after = parseInstant(Body.str(body, "startDateAfter"));
        return build(
                lat, lng, miles, after,
                Body.integer(body, "pageSize"),
                Body.integer(body, "maxPages"),
                Body.str(body, "source"),
                Body.str(body, "eventType"),
                firstPresent(body, "categoryContains", "category"),
                Body.str(body, "city"),
                Body.integer(body, "maxCostCents"),
                Body.integer(body, "minSeatsLeft"));
    }

    private static FindQuery build(
            Double lat, Double lng, Integer miles, Instant after,
            Integer pageSize, Integer maxPages, String sourceId, String eventType,
            String category, String city, Integer maxCost, Integer minSeats) {
        if (lat == null || lng == null) {
            throw new IllegalArgumentException("lat and lng are required");
        }
        if (miles == null) miles = 25;
        if (miles <= 0) throw new IllegalArgumentException("radiusMiles must be > 0");
        if (minSeats != null && minSeats < 0) throw new IllegalArgumentException("minSeatsLeft must be >= 0");
        if (maxCost != null && maxCost < 0) throw new IllegalArgumentException("maxCostCents must be >= 0");
        EventSource source = EventSources.fromId(sourceId);
        EventQuery query = new EventQuery(
                lat, lng, miles, after,
                pageSize == null ? 25 : pageSize,
                maxPages == null ? 5 : maxPages);
        return new FindQuery(source, query, eventType, category, city, maxCost, minSeats);
    }

    String filtersJson() {
        return Json.obj(
                "eventType", eventType.isBlank() ? Json.nil() : Json.str(eventType),
                "categoryContains", categoryContains.isBlank() ? Json.nil() : Json.str(categoryContains),
                "city", city.isBlank() ? Json.nil() : Json.str(city),
                "maxCostCents", maxCostCents == null ? Json.nil() : Json.num(maxCostCents),
                "minSeatsLeft", minSeatsLeft == null ? Json.nil() : Json.num(minSeatsLeft));
    }

    String queryJson() {
        return Json.obj(
                "latitude", Json.num(query.latitude),
                "longitude", Json.num(query.longitude),
                "radiusMiles", Json.num(query.radiusMiles),
                "startDateAfter", Json.str(query.startDateAfter.toString()),
                "pageSize", Json.num(query.pageSize),
                "maxPages", Json.num(query.maxPages));
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("startDateAfter must be ISO-8601 instant, e.g. 2026-09-10T00:00:00Z");
        }
    }

    private static String firstPresent(String json, String... keys) {
        for (String key : keys) {
            if (Body.has(json, key)) return Body.str(json, key);
        }
        return null;
    }
}
