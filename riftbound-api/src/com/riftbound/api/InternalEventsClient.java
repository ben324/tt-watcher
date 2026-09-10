package com.riftbound.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

final class InternalEventsClient {
    static final class Lookup {
        final int httpStatus;
        final String body;
        Lookup(int httpStatus, String body) {
            this.httpStatus = httpStatus;
            this.body = body == null ? "" : body;
        }
    }
    private final String baseUrl;
    private final String key;
    private final HttpClient http;
    InternalEventsClient(String baseUrl, String key) {
        String b = baseUrl == null || baseUrl.isBlank() ? "http://127.0.0.1:8081" : baseUrl.trim();
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        this.baseUrl = b;
        this.key = key == null || key.isBlank() ? "dev-internal" : key.trim();
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NORMAL).build();
    }
    static InternalEventsClient fromEnv() {
        return new InternalEventsClient(System.getenv("INTERNAL_BASE_URL"), System.getenv("INTERNAL_API_KEY"));
    }
    Lookup listNear(String source, double lat, double lng, int radiusMiles, String startDateAfter, int pageSize, int maxPages) throws IOException {
        String src = source == null || source.isBlank() ? "uvs-hydra" : source;
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/internal/events?source=").append(enc(src))
                .append("&lat=").append(enc(Double.toString(lat)))
                .append("&lng=").append(enc(Double.toString(lng)))
                .append("&radiusMiles=").append(radiusMiles)
                .append("&pageSize=").append(pageSize)
                .append("&maxPages=").append(maxPages);
        if (startDateAfter != null && !startDateAfter.isBlank()) url.append("&startDateAfter=").append(enc(startDateAfter));
        return send(url.toString(), 8);
    }
    Lookup getEvent(String source, String eventId) throws IOException {
        String src = source == null || source.isBlank() ? "uvs-hydra" : source;
        String url = baseUrl + "/internal/events/" + URLEncoder.encode(eventId, StandardCharsets.UTF_8) + "?source=" + URLEncoder.encode(src, StandardCharsets.UTF_8);
        return send(url, 8);
    }
    private Lookup send(String url, int seconds) throws IOException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(seconds)).header("Accept", "application/json").header("X-Internal-Key", key).GET().build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return new Lookup(res.statusCode(), res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted calling internal events", e);
        }
    }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
