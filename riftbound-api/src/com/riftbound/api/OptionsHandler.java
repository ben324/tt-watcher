package com.riftbound.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

final class OptionsHandler implements HttpHandler {
    OptionsHandler() {}
    OptionsHandler(App app) {}
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Http.preflight(ex)) return;
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            Http.methodNotAllowed(ex, "GET, OPTIONS");
            return;
        }
        Http.json(ex, 200, EventCatalog.optionsJson());
    }
}
