package com.riftbound.events;

import com.riftbound.events.playriftbound.PlayRiftboundEventSource;
import com.riftbound.events.uvs.UvsHydraEventSource;

public final class EventSources {
    public static final String UVS_HYDRA = "uvs-hydra";
    public static final String PLAY_RIFTBOUND = "playriftbound";
    private EventSources() {}
    public static EventSource uvsHydra() { return new UvsHydraEventSource(); }
    public static EventSource playRiftbound() { return new PlayRiftboundEventSource(); }
    public static EventSource fromId(String id) {
        if (id == null || id.isBlank() || UVS_HYDRA.equalsIgnoreCase(id) || "uvs".equalsIgnoreCase(id)) return uvsHydra();
        if (PLAY_RIFTBOUND.equalsIgnoreCase(id) || "play".equalsIgnoreCase(id)) return playRiftbound();
        throw new IllegalArgumentException("Unknown event source: " + id);
    }
}
