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
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    }
    static InternalEventsClient fromEnv() {
        return new InternalEventsClient(System.getenv("INTERNAL_BASE_URL"), System.getenv("INTERNAL_API_KEY"));
    }
    Lookup getEvent(String source, String eventId) throws IOException {
        String src = source == null || source.isBlank() ? "uvs-hydra" : source;
        String url = baseUrl + "/internal/events/" + URLEncoder.encode(eventId, StandardCharsets.UTF_8) + "?source=" + URLEncoder.encode(src, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).header("Accept", "application/json").header("X-Internal-Key", key).GET().build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return new Lookup(res.statusCode(), res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted calling internal events", e);
        }
    }
}
