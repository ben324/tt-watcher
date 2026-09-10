package com.riftbound.events;

import java.io.IOException;

public final class EventSourceException extends IOException {
    public final String sourceId;
    public final int httpStatus;
    public EventSourceException(String sourceId, String message) { this(sourceId, message, 0, null); }
    public EventSourceException(String sourceId, String message, int httpStatus) { this(sourceId, message, httpStatus, null); }
    public EventSourceException(String sourceId, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.sourceId = sourceId;
        this.httpStatus = httpStatus;
    }
    public static EventSourceException rateLimited(String sourceId) {
        return new EventSourceException(sourceId, sourceId + " rate-limited this client (HTTP 429). Back off.", 429);
    }
    public static EventSourceException notAvailable(String sourceId, String why) {
        return new EventSourceException(sourceId, why, 0);
    }
}
