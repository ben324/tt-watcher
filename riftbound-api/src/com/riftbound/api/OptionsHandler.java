package com.riftbound.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Optional;

final class OptionsHandler implements HttpHandler {
    private final App app;
    OptionsHandler(App app) { this.app = app; }
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Http.preflight(ex)) return;
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { Http.methodNotAllowed(ex, "GET, OPTIONS"); return; }
        Optional<User> user = app.requireUser(ex);
        if (user.isEmpty()) return;
        Http.json(ex, 200, EventCatalog.optionsJson());
    }
}
