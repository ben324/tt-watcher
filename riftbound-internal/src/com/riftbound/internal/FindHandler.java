package com.riftbound.internal;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

final class FindHandler implements HttpHandler {

    private final String key;

    FindHandler(String key) {
        this.key = key;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            Http.methodNotAllowed(ex, "POST");
            return;
        }
        if (InternalAuth.rejectIfUnauthorized(ex, key)) {
            return;
        }
        try {
            Finder.run(ex, FindQuery.fromJson(Http.readBody(ex)));
        } catch (IllegalArgumentException e) {
            Http.json(ex, 400, Json.obj(
                    "error", Json.str("bad_request"),
                    "message", Json.str(e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            Http.json(ex, 500, Json.obj(
                    "error", Json.str("internal"),
                    "message", Json.str("Find events failed")));
        }
    }
}
