package com.riftbound.events;

public final class Event {
    public final String id;
    public final String name;
    public final String store;
    public final String city;
    public final String address;
    public final String startDatetime;
    public final String eventType;
    public final String status;
    public final String categoryHint;
    public final int registered;
    public final int capacity;
    public final int costCents;
    public final String currency;
    public final String detailUrl;

    public Event(String id, String name, String store, String city, String address, String startDatetime, String eventType, String status, String categoryHint, int registered, int capacity, int costCents, String currency, String detailUrl) {
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name;
        this.store = store == null ? "" : store;
        this.city = city == null ? "" : city;
        this.address = address == null ? "" : address;
        this.startDatetime = startDatetime == null ? "" : startDatetime;
        this.eventType = eventType == null ? "" : eventType;
        this.status = status == null ? "" : status;
        this.categoryHint = categoryHint == null ? "" : categoryHint;
        this.registered = registered;
        this.capacity = capacity;
        this.costCents = costCents;
        this.currency = currency == null ? "" : currency;
        this.detailUrl = detailUrl == null ? "" : detailUrl;
    }

    public int seatsLeft() {
        if (capacity <= 0) return -1;
        return Math.max(0, capacity - registered);
    }

    @Override
    public String toString() {
        String seats = seatsLeft() < 0 ? "?" : seatsLeft() + "/" + capacity;
        return "[" + seats + "] " + id + "  " + name + " @ " + store;
    }
}
