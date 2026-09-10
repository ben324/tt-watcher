package com.riftbound.events.uvs;

import com.riftbound.events.Event;
import com.riftbound.events.EventQuery;
import com.riftbound.events.EventSource;
import com.riftbound.events.EventSourceException;
import com.riftbound.events.EventSources;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class UvsHydraEventSource implements EventSource {
    public static final String DEFAULT_BASE = "https://api.cloudflare.riftbound.uvsgames.com/hydraproxy/api/v2/events/";
    private final String baseUrl;
    private final HttpClient http;

    public UvsHydraEventSource() { this(DEFAULT_BASE); }
    public UvsHydraEventSource(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    @Override public String id() { return EventSources.UVS_HYDRA; }

    @Override public List<Event> list(EventQuery query) throws EventSourceException {
        List<Event> all = new ArrayList<>();
        try {
            for (int page = 1; page <= query.maxPages; page++) {
                String body = fetch(buildUrl(query, page));
                List<Event> chunk = UvsEventParser.parseList(body);
                if (chunk.isEmpty()) break;
                all.addAll(chunk);
                if (chunk.size() < query.pageSize) break;
            }
        } catch (EventSourceException e) { throw e; }
        catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new EventSourceException(id(), "UVS list failed: " + e.getMessage(), 0, e);
        }
        return all;
    }

    @Override public Event get(String id) throws EventSourceException {
        if (id == null || id.isBlank()) return null;
        try {
            return UvsEventParser.parseOne(fetch(baseUrl + id.trim() + "/"));
        } catch (EventSourceException e) {
            if (e.httpStatus == 404) return null;
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new EventSourceException(id(), "UVS get failed: " + e.getMessage(), 0, e);
        }
    }

    String buildUrl(EventQuery query, int page) {
        return baseUrl + "?start_date_after=" + enc(query.startDateAfter.toString())
                + "&display_statuses=upcoming&display_statuses=inProgress&game_slug=riftbound"
                + "&latitude=" + query.latitude + "&longitude=" + query.longitude
                + "&num_miles=" + query.radiusMiles + "&upcoming_only=true&page=" + page + "&page_size=" + query.pageSize;
    }

    private String fetch(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "riftbound-events/0.1 (personal library; polite poll)")
                .GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 429) throw EventSourceException.rateLimited(id());
        if (res.statusCode() < 200 || res.statusCode() >= 300) throw new EventSourceException(id(), "UVS HTTP " + res.statusCode(), res.statusCode());
        return res.body();
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
