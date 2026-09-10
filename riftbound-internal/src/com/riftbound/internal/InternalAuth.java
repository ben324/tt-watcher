package com.riftbound.internal;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;

final class InternalAuth {

    static final String DEFAULT_KEY = "dev-internal";

    private InternalAuth() {}

    static String configuredKey() {
        String env = System.getenv("INTERNAL_API_KEY");
        if (env == null || env.isBlank()) {
            return DEFAULT_KEY;
        }
        return env.trim();
    }

    static boolean rejectIfUnauthorized(HttpExchange ex, String expectedKey) throws IOException {
        String got = header(ex, "X-Internal-Key");
        if (got == null) {
            String auth = header(ex, "Authorization");
            if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
                got = auth.substring(7).trim();
            }
        }
        if (got != null && got.equals(expectedKey)) {
            return false;
        }
        Http.json(ex, 401, Json.obj(
                "error", Json.str("unauthorized"),
                "message", Json.str("Send X-Internal-Key or Authorization: Bearer <INTERNAL_API_KEY>")));
        return true;
    }

    private static String header(HttpExchange ex, String name) {
        List<String> values = ex.getRequestHeaders().get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        String v = values.get(0);
        return v == null || v.isBlank() ? null : v.trim();
    }
}
