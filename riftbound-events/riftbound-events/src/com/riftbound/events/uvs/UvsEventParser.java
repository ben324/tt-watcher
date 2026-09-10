package com.riftbound.events.uvs;

import com.riftbound.events.Event;
import com.riftbound.events.json.JsonBits;
import java.util.ArrayList;
import java.util.List;

public final class UvsEventParser {
    public static final String DETAIL_BASE = "https://locator.riftbound.uvsgames.com/events/";
    private UvsEventParser() {}
    public static List<Event> parseList(String json) {
        List<Event> events = new ArrayList<>();
        for (String obj : JsonBits.objectsInArray(json, "results")) {
            Event e = parseOne(obj);
            if (e != null && !e.id.isBlank()) events.add(e);
        }
        return events;
    }
    public static Event parseOne(String obj) {
        String id = String.valueOf(JsonBits.number(obj, "id", 0));
        if ("0".equals(id)) return null;
        String name = JsonBits.quoted(obj, "name");
        String storeObj = JsonBits.nestedObject(obj, "store");
        String store = storeObj.isBlank() ? "" : JsonBits.quoted(storeObj, "name");
        String city = storeObj.isBlank() ? "" : JsonBits.quoted(storeObj, "city");
        return new Event(id, name, store, city, JsonBits.quoted(obj, "full_address"), JsonBits.quoted(obj, "start_datetime"), JsonBits.quoted(obj, "event_type"), JsonBits.quoted(obj, "display_status"), name + "\n" + JsonBits.quoted(obj, "description"), JsonBits.number(obj, "registered_user_count", 0), JsonBits.number(obj, "capacity", 0), JsonBits.number(obj, "cost_in_cents", 0), JsonBits.quoted(obj, "currency"), DETAIL_BASE + id);
    }
}
