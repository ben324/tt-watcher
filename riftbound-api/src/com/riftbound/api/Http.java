package com.riftbound.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class Http {
    static final String JSON = "application/json; charset=utf-8";
    private Http() {}
    static void cors(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization");
        h.set("Access-Control-Max-Age", "86400");
        h.set("Access-Control-Expose-Headers", "Authorization");
    }
    static boolean preflight(HttpExchange ex) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) return false;
        cors(ex);
        ex.sendResponseHeaders(204, -1);
        ex.close();
        return true;
    }
    static void json(HttpExchange ex, int status, String body) throws IOException {
        cors(ex);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", JSON);
        h.set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
    static void methodNotAllowed(HttpExchange ex, String allow) throws IOException {
        cors(ex);
        ex.getResponseHeaders().set("Allow", allow);
        json(ex, 405, Json.obj("error", Json.str("method_not_allowed"), "message", Json.str("Use " + allow)));
    }
    static Map<String, String> query(URI uri) {
        Map<String, String> out = new HashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null || raw.isBlank()) return out;
        for (String part : raw.split("&")) {
            if (part.isEmpty()) continue;
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
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }
    static Double d(Map<String, String> q, String... keys) {
        String v = first(q, keys);
        if (v == null) return null;
        try { return Double.parseDouble(v); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("not a number: " + keys[0] + "=" + v); }
    }
    static Integer i(Map<String, String> q, String... keys) {
        String v = first(q, keys);
        if (v == null) return null;
        try { return Integer.parseInt(v); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("not an integer: " + keys[0] + "=" + v); }
    }
    static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) { return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
    }
    static String bearer(HttpExchange ex) {
        List<String> headers = ex.getRequestHeaders().get("Authorization");
        if (headers != null) {
            for (String raw : headers) {
                if (raw == null) continue;
                String v = raw.trim();
                if (v.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    String token = v.substring(7).trim();
                    if (!token.isEmpty()) return token;
                }
            }
        }
        List<String> cookies = ex.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;
        for (String header : cookies) {
            if (header == null) continue;
            for (String part : header.split(";")) {
                String p = part.trim();
                if (p.startsWith(Store.COOKIE + "=")) {
                    String token = p.substring(Store.COOKIE.length() + 1).trim();
                    if (!token.isEmpty()) return token;
                }
            }
        }
        return null;
    }
    static void setSessionCookie(HttpExchange ex, String token) {
        ex.getResponseHeaders().add("Set-Cookie", Store.COOKIE + "=" + token + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + Store.SESSION_TTL_SECONDS);
    }
    static void clearSessionCookie(HttpExchange ex) {
        ex.getResponseHeaders().add("Set-Cookie", Store.COOKIE + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
    }
    static String path(HttpExchange ex) {
        String p = ex.getRequestURI().getPath();
        if (p.length() > 1 && p.endsWith("/")) return p.substring(0, p.length() - 1);
        return p;
    }
    private static String decode(String s) { return URLDecoder.decode(s.replace("+", "%20"), StandardCharsets.UTF_8); }
}
