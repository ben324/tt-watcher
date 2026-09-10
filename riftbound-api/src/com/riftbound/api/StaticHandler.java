package com.riftbound.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

final class StaticHandler implements HttpHandler {
    private final Path root;
    StaticHandler(Path root) { this.root = root; }
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Http.preflight(ex)) return;
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod()) && !"HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
            Http.methodNotAllowed(ex, "GET, HEAD, OPTIONS");
            return;
        }
        String raw = ex.getRequestURI().getPath();
        if (raw == null || raw.equals("/") || raw.isBlank()) raw = "/index.html";
        if (raw.contains("..")) {
            Http.json(ex, 400, Json.obj("error", Json.str("bad_request"), "message", Json.str("bad path")));
            return;
        }
        Path file = root.resolve(raw.substring(1)).normalize();
        if (!file.startsWith(root.normalize()) || !Files.isRegularFile(file)) {
            Http.json(ex, 404, Json.obj("error", Json.str("not_found"), "path", Json.str(raw)));
            return;
        }
        String type = switch (extension(file.getFileName().toString())) {
            case "html" -> "text/html; charset=utf-8";
            case "css" -> "text/css; charset=utf-8";
            case "js" -> "text/javascript; charset=utf-8";
            case "svg" -> "image/svg+xml";
            case "png" -> "image/png";
            default -> "application/octet-stream";
        };
        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }
}
