package com.riftbound.internal;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class Http {

    static final String JSON = "application/json; charset=utf-8";

    private Http() {}

    static void json(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", JSON);
        h.set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static void methodNotAllowed(HttpExchange ex, String allow) throws IOException {
        ex.getResponseHeaders().set("Allow", allow);
        json(ex, 405, Json.obj(
                "error", Json.str("method_not_allowed"),
                "message", Json.str("Use " + allow)));
    }

    static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static Map<String, String> query(URI uri) {
        Map<String, String> out = new HashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            String key = eq < 0 ? part : part.substring(0, eq);
            String val = eq < 0 ? "" : part.substring(eq + 1);
            out.put(decode(key).toLowerCase(Locale.ROOT), decode(val));
        }
        return out;
    }

    static String first(Map<String, String> q, String... keys) {
        for (String key : keys) {
            String v = q.get(key.toLowerCase(Locale.ROOT));
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    static Double d(Map<String, String> q, String... keys) {
        String v = first(q, keys);
        if (v == null) {
            return null;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a number: " + keys[0] + "=" + v);
        }
    }

    static Integer i(Map<String, String> q, String... keys) {
        String v = first(q, keys);
        if (v == null) {
            return null;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not an integer: " + keys[0] + "=" + v);
        }
    }

    private static String decode(String s) {
        return URLDecoder.decode(s.replace("+", "%20"), StandardCharsets.UTF_8);
    }
}
