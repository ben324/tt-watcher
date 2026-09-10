package com.riftbound.api;

import com.riftbound.events.Event;
import java.util.List;
import java.util.Locale;

final class EventCatalog {
    static final int MAX_RADIUS_MILES = 100;
    static final String ALL = "ALL";
    record Type(String id, String label, String match) {}
    static final List<Type> TYPES = List.of(
            new Type(ALL, "Any type", ""),
            new Type("NEXUS_NIGHTS", "Nexus Nights", "nexus night"),
            new Type("NEXUS_NIGHTS_1V1", "Nexus Nights — 1v1", "nexus night"),
            new Type("OPEN_PLAY", "Riftbound Open Play", "open play"),
            new Type("LEARN_TO_PLAY", "Learn-to-Play", "learn-to-play"),
            new Type("CHAMPION_DECK_TRIAL", "Champion Deck Trial", "champion deck"),
            new Type("SEALED", "Sealed / Sealed Deck Challenge", "sealed"),
            new Type("DRAFT", "Draft / Draft Challenge", "draft"),
            new Type("CONSTRUCTED", "Constructed", "constructed"),
            new Type("PRE_RQ", "Pre-RQ Challenge", "pre-rq"),
            new Type("REGIONAL_QUALIFIER", "Regional Qualifier", "regional qualifier"),
            new Type("SUMMONER_SKIRMISH", "Summoner Skirmish", "skirmish"),
            new Type("TEAM_2V2", "2v2 / Team Challenge", "2v2"),
            new Type("SHOWDOWN", "Showdown Series", "showdown"),
            new Type("VENDETTA", "Vendetta", "vendetta"),
            new Type("LOCALS", "Locals (UVS event_type)", "locals"));
    private EventCatalog() {}
    static Type find(String id) {
        if (id == null || id.isBlank()) return TYPES.get(0);
        String needle = id.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (Type t : TYPES) {
            if (t.id.equalsIgnoreCase(needle) || t.label.equalsIgnoreCase(id.trim())) return t;
        }
        return null;
    }
    static boolean allowed(String id) { return find(id) != null; }
    static boolean matches(Event event, String typeId) {
        Type t = find(typeId);
        if (t == null || t.id.equals(ALL) || t.match.isBlank()) return true;
        String hay = (event.eventType + "\n" + event.name + "\n" + event.categoryHint).toLowerCase(Locale.ROOT);
        if (t.id.equals("LOCALS")) return "LOCALS".equalsIgnoreCase(event.eventType);
        if (t.id.equals("NEXUS_NIGHTS_1V1")) return hay.contains("nexus") && (hay.contains("1v1") || hay.contains("1 v 1"));
        if (t.id.equals("NEXUS_NIGHTS")) return hay.contains("nexus");
        return hay.contains(t.match);
    }
    static String optionsJson() {
        List<String> items = new java.util.ArrayList<>();
        for (Type t : TYPES) items.add(Json.obj("id", Json.str(t.id), "label", Json.str(t.label)));
        return Json.obj("maxRadiusMiles", Json.num(MAX_RADIUS_MILES), "eventTypes", Json.arr(items));
    }
}
