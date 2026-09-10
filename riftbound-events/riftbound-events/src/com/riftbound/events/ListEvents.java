package com.riftbound.events;

import java.util.List;

public final class ListEvents {
    public static void main(String[] args) throws Exception {
        String sourceId = EventSources.UVS_HYDRA;
        double lat = 49.2827;
        double lng = -123.1207;
        int miles = 50;
        int i = 0;
        if (args.length > 0 && !looksNumeric(args[0])) { sourceId = args[0]; i = 1; }
        if (args.length >= i + 3) {
            lat = Double.parseDouble(args[i]);
            lng = Double.parseDouble(args[i + 1]);
            miles = Integer.parseInt(args[i + 2]);
        }
        EventSource source = EventSources.fromId(sourceId);
        EventQuery query = EventQuery.near(lat, lng, miles);
        System.out.println("source=" + source.id() + " " + query);
        List<Event> events = source.list(query);
        System.out.println(events.size() + " event(s)");
        int shown = 0;
        for (Event e : events) {
            if (shown++ >= 8) { System.out.println("..."); break; }
            System.out.println("  " + e);
        }
    }
    private static boolean looksNumeric(String s) {
        return !s.isEmpty() && (s.charAt(0) == '-' || Character.isDigit(s.charAt(0)));
    }
}
