package com.riftbound.api;

import com.riftbound.events.Event;
import com.riftbound.events.json.JsonBits;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class SearchJob {
    static final int PAGE_SIZE = 10;
    static final int MAX_PAGES = 1;
    static final long PAUSE_MS = 250;
    private final Store store;
    private final InternalEventsClient events;
    SearchJob(Store store, InternalEventsClient events) {
        this.store = store;
        this.events = events;
    }
    List<Hit> run() {
        List<Hit> hits = new ArrayList<>();
        for (Watch watch : store.allWatches()) {
            if (!Watch.KIND_SEARCH.equals(watch.kind)) continue;
            try {
                hits.addAll(runOne(watch));
                Thread.sleep(PAUSE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("search job watch=" + watch.id + " failed: " + e.getMessage());
            }
        }
        return hits;
    }
    List<Hit> runOne(Watch watch) throws IOException {
        int miles = watch.radiusMiles > 0 ? watch.radiusMiles : 25;
        InternalEventsClient.Lookup lookup = events.listNear(
                watch.source, watch.latitude, watch.longitude, miles, watch.startDateAfter, PAGE_SIZE, MAX_PAGES);
        if (lookup.httpStatus < 200 || lookup.httpStatus >= 300) {
            throw new IOException("internal list HTTP " + lookup.httpStatus + " " + lookup.body);
        }
        List<Hit> hits = new ArrayList<>();
        for (String obj : JsonBits.objectsInArray(lookup.body, "events")) {
            try {
                Event event = parseEvent(obj);
                if (event.id.isBlank()) continue;
                if (!matches(watch, event)) continue;
                hits.add(new Hit(watch, event));
            } catch (Exception e) {
                System.err.println("search job watch=" + watch.id + " skipped one event: " + e.getMessage());
            }
        }
        return hits;
    }
    static boolean matches(Watch watch, Event event) {
        try {
            if (!EventCatalog.matches(event, watch.eventType)) return false;
            Instant start = event.startDatetime.isBlank() ? Instant.EPOCH : Dates.parseStart(event.startDatetime);
            Instant after = watch.startDateAfter.isBlank() ? Instant.EPOCH : Dates.parseStart(watch.startDateAfter);
            Instant before = watch.startDateBefore.isBlank() ? Instant.MAX : Dates.parseEnd(watch.startDateBefore);
            return !start.isBefore(after) && !start.isAfter(before);
        } catch (Exception e) {
            System.err.println("search job watch=" + watch.id + " event=" + event.id + " filter failed: " + e.getMessage());
            return false;
        }
    }
    static Event parseEvent(String obj) {
        return new Event(
                JsonBits.quoted(obj, "id"), JsonBits.quoted(obj, "name"), JsonBits.quoted(obj, "store"),
                JsonBits.quoted(obj, "city"), JsonBits.quoted(obj, "address"), JsonBits.quoted(obj, "startDatetime"),
                JsonBits.quoted(obj, "eventType"), JsonBits.quoted(obj, "status"), JsonBits.quoted(obj, "categoryHint"),
                JsonBits.number(obj, "registered", 0), JsonBits.number(obj, "capacity", 0),
                JsonBits.number(obj, "costCents", 0), JsonBits.quoted(obj, "currency"), JsonBits.quoted(obj, "detailUrl"));
    }
    record Hit(Watch watch, Event event) {
        String line() { return watch.id + "  " + event.id + "  " + event.name + " @ " + event.store; }
    }
}
