package com.riftbound.api;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

final class App {
    final Store store;
    final EventWatchValidator eventWatches;
    App(Path dataFile) {
        this.store = new Store(dataFile);
        this.eventWatches = new EventWatchValidator(InternalEventsClient.fromEnv());
    }
    Optional<User> requireUser(HttpExchange ex) throws IOException {
        String token = Http.bearer(ex);
        Optional<User> user = store.userForToken(token);
        if (user.isEmpty()) {
            Http.json(ex, 401, Json.obj("error", Json.str("unauthorized"), "message", Json.str("Log in and send Authorization: Bearer <token>")));
        }
        return user;
    }
}
