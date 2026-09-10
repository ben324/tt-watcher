package com.riftbound.internal;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class InternalMain {

    public static final int DEFAULT_PORT = 8081;

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            String env = System.getenv("INTERNAL_PORT");
            if (env != null && !env.isBlank()) {
                port = Integer.parseInt(env.trim());
            }
        }

        String key = InternalAuth.configuredKey();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/internal/health", new HealthHandler(key));
        server.createContext("/internal/sources", new SourcesHandler(key));
        server.createContext("/internal/events", new EventsHandler(key));
        server.createContext("/internal/find", new FindHandler(key));
        server.createContext("/", new NotFoundHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("riftbound-internal listening on http://127.0.0.1:" + port);
        System.out.println("  GET  /internal/health");
        System.out.println("  GET  /internal/sources");
        System.out.println("  GET  /internal/events?lat=&lng=&radiusMiles=");
        System.out.println("  GET  /internal/events/{id}");
        System.out.println("  POST /internal/find");
        System.out.println("  auth header X-Internal-Key (env INTERNAL_API_KEY)");
    }
}
