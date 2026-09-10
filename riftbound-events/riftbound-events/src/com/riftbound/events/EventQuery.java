package com.riftbound.events;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class EventQuery {
    public final double latitude;
    public final double longitude;
    public final int radiusMiles;
    public final Instant startDateAfter;
    public final int pageSize;
    public final int maxPages;

    public EventQuery(double latitude, double longitude, int radiusMiles, Instant startDateAfter, int pageSize, int maxPages) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) throw new IllegalArgumentException("latitude/longitude must be finite");
        if (radiusMiles <= 0) throw new IllegalArgumentException("radiusMiles must be > 0");
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMiles = radiusMiles;
        this.startDateAfter = startDateAfter == null ? Instant.now().truncatedTo(ChronoUnit.DAYS) : startDateAfter;
        this.pageSize = pageSize <= 0 ? 25 : pageSize;
        this.maxPages = maxPages <= 0 ? 5 : maxPages;
    }

    public static EventQuery near(double latitude, double longitude, int radiusMiles) {
        return new EventQuery(latitude, longitude, radiusMiles, Instant.now().truncatedTo(ChronoUnit.DAYS), 25, 5);
    }

    @Override public String toString() { return latitude + "," + longitude + " r=" + radiusMiles + "mi"; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventQuery other)) return false;
        return Double.compare(other.latitude, latitude) == 0 && Double.compare(other.longitude, longitude) == 0 && radiusMiles == other.radiusMiles && pageSize == other.pageSize && maxPages == other.maxPages && Objects.equals(startDateAfter, other.startDateAfter);
    }
    @Override public int hashCode() { return Objects.hash(latitude, longitude, radiusMiles, startDateAfter, pageSize, maxPages); }
}
