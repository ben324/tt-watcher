package com.riftbound.api;

import com.riftbound.events.Event;
import com.riftbound.events.json.JsonBits;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class SearchJob {
    static final int PAGE_SIZE = 25;
    static final int MAX_PAGES = 2;
    static final long PAUSE_MS = 250;
    private final Store store;
    private final InternalEventsClient events;
    SearchJob(Store store, InternalEventsClient events) { this.store = store; this.events = events; }
    List<Hit> run() {
        List<Hit> hits = new ArrayList<>();
        for (Watch watch : store.allWatches()) {
            if (!Watch.KIND_SEARCH.equals(watch.kind)) continue;
            try {
                hits.addAll(runOne(watch));
                Thread.sleep(PAUSE_MS);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            catch (Exception e) { System.err.println("search job watch=" + watch.id + " failed: " + e.getMessage()); }
        }
        return hits;
    }
    Watch seed(Watch watch) throws IOException { return scan(watch, true).watch; }
    List<Hit> runOne(Watch watch) throws IOException { return scan(watch, false).hits; }
    static final class Scan {
        final Watch watch; final List<Hit> hits;
        Scan(Watch watch, List<Hit> hits) { this.watch = watch; this.hits = hits; }
    }
    Scan scan(Watch watch, boolean seedOnly) throws IOException {
        int miles = watch.radiusMiles > 0 ? watch.radiusMiles : 25;
        InternalEventsClient.Lookup lookup = events.listNear(
                watch.source, watch.latitude, watch.longitude, miles, watch.startDateAfter, PAGE_SIZE, MAX_PAGES);
        if (lookup.httpStatus < 200 || lookup.httpStatus >= 300) {
            throw new IOException("internal list HTTP " + lookup.httpStatus + " " + lookup.body);
        }
        Set<String> seen = split(watch.seenIds);
        Set<String> wasFull = split(watch.fullIds);
        if (seen.isEmpty()) seedOnly = true;
        Set<String> nowSeen = new LinkedHashSet<>(seen);
        Set<String> nowFull = new LinkedHashSet<>();
        List<Hit> hits = new ArrayList<>();
        for (String obj : JsonBits.objectsInArray(lookup.body, "events")) {
            try {
                Event event = parseEvent(obj);
                if (event.id.isBlank() || !criteriaMatch(watch, event)) continue;
                boolean full = isFull(event);
                nowSeen.add(event.id);
                if (full) nowFull.add(event.id);
                if (seedOnly || full) continue;
                boolean unknown = !seen.contains(event.id);
                boolean opened = seen.contains(event.id) && wasFull.contains(event.id);
                if (unknown || opened) hits.add(new Hit(watch, event));
            } catch (Exception e) {
                System.err.println("search job watch=" + watch.id + " skipped one event: " + e.getMessage());
            }
        }
        Watch updated = watch.withSnapshot(join(nowSeen), join(nowFull));
        store.replaceWatch(watch.userId, watch.id, updated);
        List<Hit> out = new ArrayList<>();
        for (Hit hit : hits) out.add(new Hit(updated, hit.event()));
        return new Scan(updated, out);
    }
    static Set<String> split(String raw) {
        Set<String> out = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) return out;
        for (String part : raw.split(",")) { String id = part.trim(); if (!id.isBlank()) out.add(id); }
        return out;
    }
    static String join(Set<String> ids) { return String.join(",", ids); }
    static boolean matches(Watch watch, Event event) { return criteriaMatch(watch, event) && !isFull(event); }
    static boolean criteriaMatch(Watch watch, Event event) {
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
    static boolean isFull(Event event) {
        String status = event.status == null ? "" : event.status.toLowerCase();
        if (status.contains("full") || status.contains("sold") || status.contains("closed")) return true;
        return event.capacity > 0 && event.registered >= event.capacity;
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
