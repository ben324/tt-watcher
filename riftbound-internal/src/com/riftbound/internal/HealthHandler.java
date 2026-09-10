package com.riftbound.internal;

import com.riftbound.events.EventSources;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

final class HealthHandler implements HttpHandler {

    private final String key;

    HealthHandler(String key) {
        this.key = key;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())
                && !"HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
            Http.methodNotAllowed(ex, "GET, HEAD");
            return;
        }
        if (InternalAuth.rejectIfUnauthorized(ex, key)) {
            return;
        }
        Http.json(ex, 200, Json.obj(
                "ok", Json.bool(true),
                "service", Json.str("riftbound-internal"),
                "defaultSource", Json.str(EventSources.UVS_HYDRA),
                "playRiftboundReady", Json.bool(false)));
    }
}
