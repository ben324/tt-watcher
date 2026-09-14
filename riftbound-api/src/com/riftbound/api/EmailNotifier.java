package com.riftbound.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EmailNotifier {
    private final Store store;
    private final Mailer mailer;
    EmailNotifier(Store store, Mailer mailer) { this.store = store; this.mailer = mailer; }
    Result process(List<SearchJob.Hit> hits) {
        Map<String, List<SearchJob.Hit>> byUser = new LinkedHashMap<>();
        for (SearchJob.Hit hit : hits) {
            if (hit == null || hit.watch() == null) continue;
            byUser.computeIfAbsent(hit.watch().userId, k -> new ArrayList<>()).add(hit);
        }
        int sent = 0, removed = 0;
        for (Map.Entry<String, List<SearchJob.Hit>> e : byUser.entrySet()) {
            var user = store.findUserById(e.getKey());
            if (user.isEmpty()) continue;
            User u = user.get();
            if (u.notifyEmail && u.email != null && !u.email.isBlank()) {
                String subject = e.getValue().size() == 1
                        ? "tt-watcher: " + e.getValue().get(0).event().name
                        : "tt-watcher: " + e.getValue().size() + " matching events";
                String body = render(u, e.getValue());
                try {
                    if (mailer == null) {
                        System.out.println("notify (no SMTP) to=" + u.email + " subject=" + subject);
                        System.out.println(body);
                    } else {
                        mailer.send(u.email, subject, body);
                        System.out.println("notify mailed " + u.email + " hits=" + e.getValue().size());
                    }
                    sent++;
                } catch (Exception ex) {
                    System.err.println("notify failed " + u.email + ": " + ex.getMessage());
                }
            }
            Set<String> ids = new LinkedHashSet<>();
            for (SearchJob.Hit hit : e.getValue()) {
                if (Watch.KIND_SEARCH.equals(hit.watch().kind)) continue;
                ids.add(hit.watch().id);
            }
            for (String watchId : ids) {
                if (store.deleteWatch(u.id, watchId)) {
                    System.out.println("removed watch " + watchId + " after hit");
                    removed++;
                }
            }
        }
        return new Result(sent, removed);
    }
    int send(List<SearchJob.Hit> hits) { return process(hits).sent; }
    static String render(User user, List<SearchJob.Hit> hits) {
        StringBuilder b = new StringBuilder();
        b.append("Matches for ").append(user.email).append("\n\n");
        for (SearchJob.Hit hit : hits) {
            var ev = hit.event();
            b.append("- ").append(ev.name).append("\n");
            if (!ev.store.isBlank() || !ev.city.isBlank()) {
                b.append("  ").append(ev.store);
                if (!ev.city.isBlank()) b.append(" · ").append(ev.city);
                b.append("\n");
            }
            if (!ev.startDatetime.isBlank()) b.append("  ").append(ev.startDatetime).append("\n");
            if (!ev.detailUrl.isBlank()) b.append("  ").append(ev.detailUrl).append("\n");
            b.append("\n");
        }
        b.append("https://ttwatcher.com\n");
        return b.toString();
    }
    record Result(int sent, int removed) {}
}
