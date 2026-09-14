package com.riftbound.api;

final class Watch {
    static final String KIND_SEARCH = "SEARCH";
    static final String KIND_EVENT = "EVENT";
    final String id, userId, kind, name, eventId, source, eventType, categoryContains, city, startDateAfter, startDateBefore, seenIds, fullIds, createdAt;
    final double latitude, longitude;
    final int radiusMiles;
    final Integer maxCostCents, minSeatsLeft;
    Watch(String id, String userId, String kind, String name, String eventId, double latitude, double longitude,
            int radiusMiles, String source, String eventType, String categoryContains, String city,
            Integer maxCostCents, Integer minSeatsLeft, String startDateAfter, String startDateBefore,
            String seenIds, String fullIds, String createdAt) {
        this.id = id; this.userId = userId;
        this.kind = kind == null || kind.isBlank() ? KIND_SEARCH : kind;
        this.name = name == null ? "" : name;
        this.eventId = eventId == null ? "" : eventId;
        this.latitude = latitude; this.longitude = longitude; this.radiusMiles = radiusMiles;
        this.source = source;
        this.eventType = eventType == null ? "" : eventType;
        this.categoryContains = categoryContains == null ? "" : categoryContains;
        this.city = city == null ? "" : city;
        this.maxCostCents = maxCostCents; this.minSeatsLeft = minSeatsLeft;
        this.startDateAfter = startDateAfter == null ? "" : startDateAfter;
        this.startDateBefore = startDateBefore == null ? "" : startDateBefore;
        this.seenIds = seenIds == null ? "" : seenIds;
        this.fullIds = fullIds == null ? "" : fullIds;
        this.createdAt = createdAt;
    }
    Watch withSnapshot(String seenIds, String fullIds) {
        return new Watch(id, userId, kind, name, eventId, latitude, longitude, radiusMiles, source, eventType,
                categoryContains, city, maxCostCents, minSeatsLeft, startDateAfter, startDateBefore, seenIds, fullIds, createdAt);
    }
    boolean isEventWatch() { return KIND_EVENT.equals(kind); }
    String publicJson() {
        return Json.obj("id", Json.str(id), "kind", Json.str(kind), "name", Json.str(name),
                "eventId", eventId.isBlank() ? Json.nil() : Json.str(eventId),
                "latitude", Json.num(latitude), "longitude", Json.num(longitude), "radiusMiles", Json.num(radiusMiles),
                "source", Json.str(source), "eventType", eventType.isBlank() ? Json.nil() : Json.str(eventType),
                "categoryContains", categoryContains.isBlank() ? Json.nil() : Json.str(categoryContains),
                "city", city.isBlank() ? Json.nil() : Json.str(city),
                "maxCostCents", maxCostCents == null ? Json.nil() : Json.num(maxCostCents),
                "minSeatsLeft", minSeatsLeft == null ? Json.nil() : Json.num(minSeatsLeft),
                "startDateAfter", startDateAfter.isBlank() ? Json.nil() : Json.str(startDateAfter),
                "startDateBefore", startDateBefore.isBlank() ? Json.nil() : Json.str(startDateBefore),
                "seenCount", Json.num(seenIds.isBlank() ? 0 : seenIds.split(",").length),
                "createdAt", Json.str(createdAt));
    }
    String storedJson() {
        return Json.obj("id", Json.str(id), "userId", Json.str(userId), "kind", Json.str(kind), "name", Json.str(name),
                "eventId", Json.str(eventId), "latitude", Json.num(latitude), "longitude", Json.num(longitude),
                "radiusMiles", Json.num(radiusMiles), "source", Json.str(source), "eventType", Json.str(eventType),
                "categoryContains", Json.str(categoryContains), "city", Json.str(city),
                "maxCostCents", maxCostCents == null ? Json.nil() : Json.num(maxCostCents),
                "minSeatsLeft", minSeatsLeft == null ? Json.nil() : Json.num(minSeatsLeft),
                "startDateAfter", Json.str(startDateAfter), "startDateBefore", Json.str(startDateBefore),
                "seenIds", Json.str(seenIds), "fullIds", Json.str(fullIds), "createdAt", Json.str(createdAt));
    }
    static Watch fromJson(String obj) {
        Double lat = Body.decimal(obj, "latitude"); Double lng = Body.decimal(obj, "longitude");
        Integer miles = Body.integer(obj, "radiusMiles");
        String kind = Body.strOrEmpty(obj, "kind"); String eventId = Body.strOrEmpty(obj, "eventId");
        if (kind.isBlank()) kind = eventId.isBlank() ? KIND_SEARCH : KIND_EVENT;
        return new Watch(Body.strOrEmpty(obj, "id"), Body.strOrEmpty(obj, "userId"), kind, Body.strOrEmpty(obj, "name"),
                eventId, lat == null ? 0 : lat, lng == null ? 0 : lng, miles == null ? 25 : miles,
                Body.strOrEmpty(obj, "source"), Body.strOrEmpty(obj, "eventType"), Body.strOrEmpty(obj, "categoryContains"),
                Body.strOrEmpty(obj, "city"), Body.integer(obj, "maxCostCents"), Body.integer(obj, "minSeatsLeft"),
                Body.strOrEmpty(obj, "startDateAfter"), Body.strOrEmpty(obj, "startDateBefore"),
                Body.strOrEmpty(obj, "seenIds"), Body.strOrEmpty(obj, "fullIds"), Body.strOrEmpty(obj, "createdAt"));
    }
}
