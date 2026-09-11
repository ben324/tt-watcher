package com.riftbound.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

final class Dates {
    private Dates() {}
    static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return parseStart(raw.trim()).toString();
    }
    static Instant parseStart(String raw) {
        if (raw == null || raw.isBlank()) return Instant.EPOCH;
        try { return Instant.parse(raw); }
        catch (DateTimeParseException e) {
            try { return LocalDate.parse(raw).atStartOfDay().toInstant(ZoneOffset.UTC); }
            catch (DateTimeParseException e2) { throw new IllegalArgumentException("dates must be YYYY-MM-DD or an ISO instant"); }
        }
    }
    static Instant parseEnd(String raw) {
        if (raw == null || raw.isBlank()) return Instant.MAX;
        try { return Instant.parse(raw); }
        catch (DateTimeParseException e) {
            try { return LocalDate.parse(raw).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusSeconds(1); }
            catch (DateTimeParseException e2) { throw new IllegalArgumentException("dates must be YYYY-MM-DD or an ISO instant"); }
        }
    }
    static boolean inRange(String eventStart, String after, String before) {
        Instant start;
        try { start = Instant.parse(eventStart); } catch (RuntimeException e) { return true; }
        if (after != null && !after.isBlank() && start.isBefore(parseStart(after))) return false;
        if (before != null && !before.isBlank() && start.isAfter(parseEnd(before))) return false;
        return true;
    }
}
