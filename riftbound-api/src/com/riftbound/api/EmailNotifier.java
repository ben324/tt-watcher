package com.riftbound.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EmailNotifier {
    private final Store store;
    private final Mailer mailer;
    EmailNotifier(Store store, Mailer mailer) { this.store = store; this.mailer = mailer; }
    int send(List<SearchJob.Hit> hits) {
        Map<String, List<SearchJob.Hit>> byUser = new LinkedHashMap<>();
        for (SearchJob.Hit hit : hits) {
            if (hit == null || hit.watch() == null) continue;
            byUser.computeIfAbsent(hit.watch().userId, k -> new ArrayList<>()).add(hit);
        }
        int sent = 0;
        for (Map.Entry<String, List<SearchJob.Hit>> e : byUser.entrySet()) {
            var user = store.findUserById(e.getKey());
            if (user.isEmpty()) continue;
            User u = user.get();
            if (!u.notifyEmail || u.email == null || u.email.isBlank()) continue;
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
        return sent;
    }
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
}
