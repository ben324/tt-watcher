package com.riftbound.internal;

import com.riftbound.events.EventSources;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

final class SourcesHandler implements HttpHandler {

    private final String key;

    SourcesHandler(String key) {
        this.key = key;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            Http.methodNotAllowed(ex, "GET");
            return;
        }
        if (InternalAuth.rejectIfUnauthorized(ex, key)) {
            return;
        }
        Http.json(ex, 200, Json.obj(
                "defaultSource", Json.str(EventSources.UVS_HYDRA),
                "sources", Json.arr(List.of(
                        Json.obj(
                                "id", Json.str(EventSources.UVS_HYDRA),
                                "ready", Json.bool(true),
                                "notes", Json.str("UVS Hydra list used by locator.riftbound.uvsgames.com")),
                        Json.obj(
                                "id", Json.str(EventSources.PLAY_RIFTBOUND),
                                "ready", Json.bool(false),
                                "notes", Json.str(
                                        "Stub until the PlayRiftbound list API exists (site launch 2026-09-14)"))))));
    }
}
