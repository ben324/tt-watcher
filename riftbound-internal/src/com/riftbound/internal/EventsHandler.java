package com.riftbound.internal;

import com.riftbound.events.Event;
import com.riftbound.events.EventSource;
import com.riftbound.events.EventSourceException;
import com.riftbound.events.EventSources;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

final class EventsHandler implements HttpHandler {

    private final String key;

    EventsHandler(String key) {
        this.key = key;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            Http.methodNotAllowed(ex, "GET");
            return;
        }
        if (InternalAuth.rejectIfUnauthorized(ex, key)) {
            return;
        }
        try {
            String eventId = eventId(ex.getRequestURI().getPath());
            if (eventId != null) {
                one(ex, eventId);
                return;
            }
            Finder.run(ex, FindQuery.fromQueryString(Http.query(ex.getRequestURI())));
        } catch (IllegalArgumentException e) {
            Http.json(ex, 400, Json.obj(
                    "error", Json.str("bad_request"),
                    "message", Json.str(e.getMessage())));
        } catch (EventSourceException e) {
            Http.json(ex, EventJson.statusFor(e), EventJson.sourceError(e));
        } catch (Exception e) {
            e.printStackTrace();
            Http.json(ex, 500, Json.obj(
                    "error", Json.str("internal"),
                    "message", Json.str("Find events failed")));
        }
    }

    private void one(HttpExchange ex, String eventId) throws IOException, EventSourceException {
        Map<String, String> q = Http.query(ex.getRequestURI());
        EventSource source = EventSources.fromId(Http.first(q, "source", "sourceId"));
        Event event = source.get(eventId);
        if (event == null) {
            Http.json(ex, 404, Json.obj(
                    "error", Json.str("not_found"),
                    "source", Json.str(source.id()),
                    "eventId", Json.str(eventId),
                    "message", Json.str("No event with that id")));
            return;
        }
        Http.json(ex, 200, Json.obj(
                "source", Json.str(source.id()),
                "event", EventJson.one(event)));
    }

    private static String eventId(String path) {
        String prefix = "/internal/events";
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        if (!path.startsWith(prefix)) {
            return null;
        }
        String rest = path.substring(prefix.length());
        if (rest.startsWith("/")) {
            rest = rest.substring(1);
        }
        return rest.isEmpty() ? null : rest;
    }
}
