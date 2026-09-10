package com.riftbound.api;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

final class EventWatchValidator {
    static final class Rejected extends Exception {
        final int httpStatus;
        final String code;
        Rejected(int httpStatus, String code, String message) {
            super(message);
            this.httpStatus = httpStatus;
            this.code = code;
        }
    }
    private final InternalEventsClient client;
    EventWatchValidator(InternalEventsClient client) { this.client = client; }
    void requireUpcoming(Watch watch) throws Rejected, IOException {
        if (!watch.isEventWatch()) return;
        InternalEventsClient.Lookup hit = client.getEvent(watch.source, watch.eventId);
        if (hit.httpStatus == 404) throw new Rejected(400, "invalid_event", "No upcoming event with that id");
        if (hit.httpStatus == 401 || hit.httpStatus == 403) throw new Rejected(502, "internal_auth", "Could not reach the internal event service");
        if (hit.httpStatus == 503) throw new Rejected(503, "source_unavailable", "That event source is not available yet");
        if (hit.httpStatus < 200 || hit.httpStatus >= 300) throw new Rejected(502, "internal_error", "Internal event lookup failed");
        String eventObj = eventObject(hit.body);
        if (eventObj.isBlank()) throw new Rejected(400, "invalid_event", "No upcoming event with that id");
        String status = Body.strOrEmpty(eventObj, "status").toLowerCase(Locale.ROOT);
        if (status.contains("progress")) throw new Rejected(400, "event_in_progress", "That event is already in progress");
        if (isFinished(status)) throw new Rejected(400, "event_over", "That event has already happened");
        if (alreadyStarted(Body.strOrEmpty(eventObj, "startDatetime"))) throw new Rejected(400, "event_over", "That event has already started");
        if (!status.isBlank() && !"upcoming".equals(status)) throw new Rejected(400, "event_not_upcoming", "That event is not an upcoming listing");
    }
    private static boolean isFinished(String status) {
        return status.contains("complete") || status.contains("completed") || status.contains("past") || status.contains("ended") || status.contains("cancel");
    }
    private static boolean alreadyStarted(String startDatetime) {
        if (startDatetime == null || startDatetime.isBlank()) return false;
        try { return Instant.parse(startDatetime).isBefore(Instant.now()); }
        catch (DateTimeParseException e) {
            try { return Instant.parse(startDatetime.replace(" ", "T")).isBefore(Instant.now()); }
            catch (DateTimeParseException e2) { return false; }
        }
    }
    private static String eventObject(String body) {
        if (body == null || body.isBlank()) return "";
        String nested = nested(body, "event");
        return nested.isBlank() ? body : nested;
    }
    private static String nested(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return "";
        i += needle.length();
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i) != '{') return "";
        int depth = 0; boolean inStr = false, escape = false;
        for (int p = i; p < json.length(); p++) {
            char c = json.charAt(p);
            if (inStr) {
                if (escape) escape = false;
                else if (c == '\\') escape = true;
                else if (c == '"') inStr = false;
                continue;
            }
            if (c == '"') inStr = true;
            else if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return json.substring(i, p + 1); }
        }
        return "";
    }
}
