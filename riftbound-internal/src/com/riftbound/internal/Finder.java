package com.riftbound.internal;

import com.riftbound.events.Event;
import com.riftbound.events.EventSourceException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;

final class Finder {

    private Finder() {}

    static void run(HttpExchange ex, FindQuery q) throws IOException {
        try {
            List<Event> fetched = q.source.list(q.query);
            List<Event> matched = EventFilter.apply(fetched, q);
            Http.json(ex, 200, EventJson.list(q, fetched, matched));
        } catch (EventSourceException e) {
            Http.json(ex, EventJson.statusFor(e), EventJson.sourceError(e));
        }
    }
}
