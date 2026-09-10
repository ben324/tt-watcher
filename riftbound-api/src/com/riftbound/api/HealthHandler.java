package com.riftbound.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

final class HealthHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (Http.preflight(ex)) return;
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod()) && !"HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
            Http.methodNotAllowed(ex, "GET, HEAD, OPTIONS");
            return;
        }
        Http.json(ex, 200, Json.obj("ok", Json.bool(true), "service", Json.str("riftbound-api")));
    }
}
