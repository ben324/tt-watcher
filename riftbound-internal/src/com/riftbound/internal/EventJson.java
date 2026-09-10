package com.riftbound.internal;

import com.riftbound.events.Event;
import com.riftbound.events.EventSourceException;

import java.util.ArrayList;
import java.util.List;

final class EventJson {

    private EventJson() {}

    static String list(FindQuery q, List<Event> fetched, List<Event> matched) {
        List<String> items = new ArrayList<>(matched.size());
        for (Event e : matched) {
            items.add(one(e));
        }
        return Json.obj(
                "source", Json.str(q.source.id()),
                "query", q.queryJson(),
                "filters", q.filtersJson(),
                "fetched", Json.num(fetched.size()),
                "count", Json.num(matched.size()),
                "events", Json.arr(items));
    }

    static String one(Event e) {
        return Json.obj(
                "id", Json.str(e.id),
                "name", Json.str(e.name),
                "store", Json.str(e.store),
                "city", Json.str(e.city),
                "address", Json.str(e.address),
                "startDatetime", Json.str(e.startDatetime),
                "eventType", Json.str(e.eventType),
                "status", Json.str(e.status),
                "categoryHint", Json.str(e.categoryHint),
                "registered", Json.num(e.registered),
                "capacity", Json.num(e.capacity),
                "seatsLeft", Json.num(e.seatsLeft()),
                "costCents", Json.num(e.costCents),
                "currency", Json.str(e.currency),
                "detailUrl", Json.str(e.detailUrl));
    }

    static int statusFor(EventSourceException e) {
        if (e.httpStatus == 429) {
            return 429;
        }
        if (e.httpStatus >= 400 && e.httpStatus < 600) {
            return 502;
        }
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("not wired") || msg.contains("not available")) {
            return 503;
        }
        return 502;
    }

    static String sourceError(EventSourceException e) {
        return Json.obj(
                "error", Json.str(e.httpStatus == 429 ? "rate_limited" : "source_error"),
                "source", Json.str(e.sourceId),
                "httpStatus", Json.num(e.httpStatus),
                "message", Json.str(e.getMessage()));
    }
}
