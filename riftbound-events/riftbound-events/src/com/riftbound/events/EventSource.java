package com.riftbound.events;

import java.util.List;

public interface EventSource {
    String id();
    List<Event> list(EventQuery query) throws EventSourceException;
    Event get(String id) throws EventSourceException;
}
