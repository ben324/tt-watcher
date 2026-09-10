package com.riftbound.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class GeocodeHandler implements HttpHandler {
    private static final String BASE = "https://api.geoapify.com/v1/geocode";
    private final App app;
    private final HttpClient http;
    GeocodeHandler(App app) {
        this.app = app;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
    }
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Http.preflight(ex)) return;
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { Http.methodNotAllowed(ex, "GET, OPTIONS"); return; }
        if (app.requireUser(ex).isEmpty()) return;
        try {
            Map<String, String> q = Http.query(ex.getRequestURI());
            if (Http.path(ex).endsWith("/reverse")) reverse(ex, q); else forward(ex, q);
        } catch (IllegalArgumentException e) {
            Http.json(ex, 400, Json.obj("error", Json.str("bad_request"), "message", Json.str(e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            Http.json(ex, 502, Json.obj("error", Json.str("geocode_failed"), "message", Json.str(e.getMessage() == null ? "Could not look up that address" : e.getMessage())));
        }
    }
    private void forward(HttpExchange ex, Map<String, String> q) throws Exception {
        String query = Http.first(q, "q", "address", "query", "text");
        if (query == null || query.isBlank()) throw new IllegalArgumentException("q is required");
        double[] pair = parseLatLng(query);
        if (pair != null) {
            Http.json(ex, 200, Json.obj("label", Json.str(pair[0] + ", " + pair[1]), "latitude", Json.num(pair[0]), "longitude", Json.num(pair[1])));
            return;
        }
        String url = BASE + "/search?format=json&limit=1&text=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8) + "&apiKey=" + URLEncoder.encode(requireKey(ex), StandardCharsets.UTF_8);
        String body = get(url);
        String first = firstObject(body);
        if (first == null) {
            Http.json(ex, 404, Json.obj("error", Json.str("not_found"), "message", Json.str("No match for that address")));
            return;
        }
        double lat = parseDouble(firstNonEmpty(first, "lat", "latitude"));
        double lng = parseDouble(firstNonEmpty(first, "lon", "lng", "longitude"));
        String label = firstNonEmpty(first, "formatted", "address_line1", "name");
        Http.json(ex, 200, Json.obj("label", label.isBlank() ? Json.nil() : Json.str(label), "latitude", Json.num(lat), "longitude", Json.num(lng)));
    }
    private void reverse(HttpExchange ex, Map<String, String> q) throws Exception {
        Double lat = Http.d(q, "lat", "latitude");
        Double lng = Http.d(q, "lng", "lon", "longitude");
        if (lat == null || lng == null) throw new IllegalArgumentException("lat and lng are required");
        String url = BASE + "/reverse?format=json&lat=" + lat + "&lon=" + lng + "&apiKey=" + URLEncoder.encode(requireKey(ex), StandardCharsets.UTF_8);
        String body = get(url);
        String first = firstObject(body);
        String label = first == null ? "" : firstNonEmpty(first, "formatted", "address_line1", "name");
        Http.json(ex, 200, Json.obj("label", label.isBlank() ? Json.nil() : Json.str(label), "latitude", Json.num(lat), "longitude", Json.num(lng)));
    }
    private String requireKey(HttpExchange ex) {
        String env = System.getenv("GEOAPIFY_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        List<String> values = ex.getRequestHeaders().get("X-Geoapify-Key");
        if (values != null && !values.isEmpty() && values.get(0) != null && !values.get(0).isBlank()) return values.get(0).trim();
        throw new IllegalArgumentException("Add a Geoapify API key at https://myprojects.geoapify.com and paste it in the form, or set GEOAPIFY_API_KEY");
    }
    private String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12)).header("Accept", "application/json").GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 401 || res.statusCode() == 403) throw new IllegalArgumentException("Geoapify rejected the API key");
        if (res.statusCode() >= 400) throw new IOException("geoapify HTTP " + res.statusCode());
        return res.body() == null ? "" : res.body();
    }
    private static String firstNonEmpty(String obj, String... keys) {
        for (String key : keys) { String v = Body.strOrEmpty(obj, key); if (v != null && !v.isBlank()) return v; }
        return "";
    }
    private static String firstObject(String json) {
        if (json == null) return null;
        String trimmed = json.trim();
        int start = trimmed.indexOf('{');
        if (trimmed.contains("\"results\"")) start = trimmed.indexOf('{', trimmed.indexOf("\"results\"") + 9);
        if (start < 0) return null;
        int depth = 0;
        for (int i = start; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return trimmed.substring(start, i + 1); }
        }
        return null;
    }
    private static double[] parseLatLng(String raw) {
        String s = raw.trim();
        if (!s.matches("^-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?$")) return null;
        String[] parts = s.split(",");
        try { return new double[] { Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()) }; }
        catch (NumberFormatException e) { return null; }
    }
    private static double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        try { return Double.parseDouble(raw.trim()); } catch (NumberFormatException e) { return 0; }
    }
}
