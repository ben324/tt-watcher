package com.riftbound.api;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public final class ApiMain {
    public static final int DEFAULT_PORT = 8080;
    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) port = Integer.parseInt(args[0]);
        else {
            String env = System.getenv("PORT");
            if (env != null && !env.isBlank()) port = Integer.parseInt(env.trim());
        }
        Path dataFile = dataFile();
        App app = new App(dataFile);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        AuthHandler auth = new AuthHandler(app);
        server.createContext("/api/auth", auth);
        server.createContext("/api/me", auth);
        server.createContext("/api/watches", new WatchesHandler(app));
        server.createContext("/api/events", new EventsHandler(app));
        server.createContext("/api/options", new OptionsHandler(app));
        Path web = webRoot();
        if (Files.isDirectory(web)) server.createContext("/", new StaticHandler(web));
        else server.createContext("/", new NotFoundHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("riftbound-api listening on http://127.0.0.1:" + port);
        System.out.println("  store=" + dataFile.toAbsolutePath());
        if (Files.isDirectory(web)) System.out.println("  UI   /  (" + web.toAbsolutePath().normalize() + ")");
    }
    static Path dataFile() {
        String override = System.getenv("RIFTBOUND_STORE");
        if (override != null && !override.isBlank()) return Path.of(override);
        return Path.of("data", "store.json");
    }
    static Path webRoot() {
        String override = System.getenv("TT_WATCHER_DIR");
        if (override != null && !override.isBlank()) return Path.of(override);
        Path sibling = Path.of("..", "tt-watcher");
        if (Files.isDirectory(sibling)) return sibling;
        return Path.of("tt-watcher");
    }
}
