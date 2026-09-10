package com.riftbound.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

final class NotFoundHandler implements HttpHandler {
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Http.preflight(ex)) return;
        Http.json(ex, 404, Json.obj("error", Json.str("not_found"), "path", Json.str(ex.getRequestURI().getPath()), "message", Json.str("Known routes: /api/auth/* /api/watches /api/events /api/options")));
    }
}
