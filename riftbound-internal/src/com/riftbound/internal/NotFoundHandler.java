package com.riftbound.internal;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

final class NotFoundHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Http.json(ex, 404, Json.obj(
                "error", Json.str("not_found"),
                "path", Json.str(ex.getRequestURI().getPath()),
                "message", Json.str(
                        "Job routes: /internal/health /internal/sources /internal/events /internal/find")));
    }
}
