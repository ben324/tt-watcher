package com.riftbound.events.playriftbound;

import com.riftbound.events.Event;
import com.riftbound.events.EventQuery;
import com.riftbound.events.EventSource;
import com.riftbound.events.EventSourceException;
import com.riftbound.events.EventSources;
import java.util.List;

public final class PlayRiftboundEventSource implements EventSource {
    public static final String DEFAULT_BASE = "https://playriftbound.com/api/events";
    private final String baseUrl;
    public PlayRiftboundEventSource() { this(DEFAULT_BASE); }
    public PlayRiftboundEventSource(String baseUrl) { this.baseUrl = baseUrl; }
    @Override public String id() { return EventSources.PLAY_RIFTBOUND; }
    @Override public List<Event> list(EventQuery query) throws EventSourceException {
        throw EventSourceException.notAvailable(id(), "PlayRiftbound list API is not wired yet. Site launches 2026-09-14; implement buildUrl + parse in " + PlayRiftboundEventSource.class.getName() + " (base=" + baseUrl + ", query=" + query + ").");
    }
    @Override public Event get(String id) throws EventSourceException {
        throw EventSourceException.notAvailable(id(), "PlayRiftbound get-by-id is not wired yet (id=" + id + ").");
    }
    String buildUrl(EventQuery query, int page) {
        return baseUrl + "?lat=" + query.latitude + "&lng=" + query.longitude + "&radiusMiles=" + query.radiusMiles + "&page=" + page;
    }
    List<Event> parse(String body) { return List.of(); }
}
