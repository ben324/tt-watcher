package com.riftbound.internal;

import com.riftbound.events.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class EventFilter {

    private EventFilter() {}

    static List<Event> apply(List<Event> events, FindQuery q) {
        List<Event> out = new ArrayList<>();
        for (Event e : events) {
            if (matches(e, q)) {
                out.add(e);
            }
        }
        return out;
    }

    static boolean matches(Event e, FindQuery q) {
        if (!q.eventType.isBlank() && !q.eventType.equalsIgnoreCase(e.eventType)) {
            return false;
        }
        if (!q.categoryContains.isBlank()) {
            String hay = (e.name + "\n" + e.categoryHint).toLowerCase(Locale.ROOT);
            if (!hay.contains(q.categoryContains.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        if (!q.city.isBlank()) {
            String hay = (e.city + " " + e.address).toLowerCase(Locale.ROOT);
            if (!hay.contains(q.city.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        if (q.maxCostCents != null && e.costCents > q.maxCostCents) {
            return false;
        }
        if (q.minSeatsLeft != null) {
            int left = e.seatsLeft();
            if (left >= 0 && left < q.minSeatsLeft) {
                return false;
            }
        }
        return true;
    }
}
