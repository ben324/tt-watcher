package com.riftbound.api;

import com.riftbound.events.json.JsonBits;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

final class Store {
    static final String COOKIE = "rb_token";
    static final int SESSION_TTL_SECONDS = (int) Duration.ofDays(14).toSeconds();
    private final Path file;
    private final List<User> users = new ArrayList<>();
    private final List<Session> sessions = new ArrayList<>();
    private final List<Watch> watches = new ArrayList<>();
    private long loadedAt;
    Store(Path file) { this.file = file; load(); }
    private void reloadIfStale() {
        try {
            if (!Files.exists(file)) return;
            long m = Files.getLastModifiedTime(file).toMillis();
            if (m <= loadedAt) return;
            users.clear(); sessions.clear(); watches.clear();
            load();
        } catch (IOException e) { System.err.println("store reload failed: " + e.getMessage()); }
    }
    synchronized User createUser(String email, char[] password) {
        reloadIfStale();
        String normalized = normalizeEmail(email);
        if (findUserByEmail(normalized).isPresent()) throw new IllegalStateException("email_taken");
        byte[] salt = Passwords.salt(); byte[] hash = Passwords.hash(password, salt);
        User user = new User(UUID.randomUUID().toString(), normalized, "", true, false,
                Passwords.hex(salt), Passwords.hex(hash), Instant.now().toString());
        users.add(user); persist(); return user;
    }
    synchronized Optional<User> authenticate(String email, char[] password) {
        reloadIfStale();
        Optional<User> found = findUserByEmail(normalizeEmail(email));
        if (found.isEmpty()) return Optional.empty();
        User user = found.get();
        if (!Passwords.verify(password, hexToBytes(user.passwordSaltHex), hexToBytes(user.passwordHashHex))) return Optional.empty();
        return Optional.of(user);
    }
    synchronized Session createSession(String userId) {
        reloadIfStale();
        Instant now = Instant.now();
        Session session = new Session(Passwords.hex(Passwords.tokenBytes()), userId, now.toString(), now.plusSeconds(SESSION_TTL_SECONDS).toString());
        sessions.add(session); persist(); return session;
    }
    synchronized Optional<User> userForToken(String token) {
        reloadIfStale();
        if (token == null || token.isBlank()) return Optional.empty();
        Session match = null;
        for (Session s : sessions) { if (s.token.equals(token)) { match = s; break; } }
        if (match == null) return Optional.empty();
        try { if (Instant.parse(match.expiresAt).isBefore(Instant.now())) { sessions.remove(match); persist(); return Optional.empty(); } }
        catch (Exception e) { return Optional.empty(); }
        return findUserById(match.userId);
    }
    synchronized boolean deleteSession(String token) {
        reloadIfStale();
        if (token == null) return false;
        boolean removed = sessions.removeIf(s -> s.token.equals(token));
        if (removed) persist();
        return removed;
    }
    synchronized Optional<User> replaceUser(User updated) {
        reloadIfStale();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).id.equals(updated.id)) { users.set(i, updated); persist(); return Optional.of(updated); }
        }
        return Optional.empty();
    }
    synchronized Watch addWatch(Watch watch) { reloadIfStale(); watches.add(watch); persist(); return watch; }
    synchronized List<Watch> allWatches() { reloadIfStale(); return new ArrayList<>(watches); }
    synchronized List<Watch> watchesFor(String userId) {
        reloadIfStale();
        List<Watch> out = new ArrayList<>();
        for (Watch w : watches) if (w.userId.equals(userId)) out.add(w);
        return out;
    }
    synchronized Optional<Watch> watchFor(String userId, String watchId) {
        reloadIfStale();
        for (Watch w : watches) if (w.userId.equals(userId) && w.id.equals(watchId)) return Optional.of(w);
        return Optional.empty();
    }
    synchronized Optional<Watch> replaceWatch(String userId, String watchId, Watch updated) {
        reloadIfStale();
        for (int i = 0; i < watches.size(); i++) {
            Watch w = watches.get(i);
            if (w.userId.equals(userId) && w.id.equals(watchId)) { watches.set(i, updated); persist(); return Optional.of(updated); }
        }
        return Optional.empty();
    }
    synchronized boolean deleteWatch(String userId, String watchId) {
        reloadIfStale();
        Iterator<Watch> it = watches.iterator();
        while (it.hasNext()) {
            Watch w = it.next();
            if (w.userId.equals(userId) && w.id.equals(watchId)) { it.remove(); persist(); return true; }
        }
        return false;
    }
    synchronized Optional<User> findUserById(String id) {
        reloadIfStale();
        for (User u : users) if (u.id.equals(id)) return Optional.of(u);
        return Optional.empty();
    }
    synchronized Optional<User> findUserByEmail(String email) {
        reloadIfStale();
        for (User u : users) if (u.email.equals(email)) return Optional.of(u);
        return Optional.empty();
    }
    static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private void load() {
        loadedAt = 0;
        if (!Files.exists(file)) { loadedAt = System.currentTimeMillis(); return; }
        try {
            loadedAt = Files.getLastModifiedTime(file).toMillis();
            String json = Files.readString(file, StandardCharsets.UTF_8);
            for (String obj : JsonBits.objectsInArray(json, "users")) {
                try { User u = User.fromJson(obj); if (!u.id.isBlank() && !u.email.isBlank()) users.add(u); }
                catch (Exception e) { System.err.println("store skipped a user: " + e.getMessage()); }
            }
            for (String obj : JsonBits.objectsInArray(json, "sessions")) {
                try { Session s = Session.fromJson(obj); if (!s.token.isBlank() && !s.userId.isBlank()) sessions.add(s); }
                catch (Exception e) { System.err.println("store skipped a session: " + e.getMessage()); }
            }
            for (String obj : JsonBits.objectsInArray(json, "watches")) {
                try { Watch w = Watch.fromJson(obj); if (!w.id.isBlank() && !w.userId.isBlank()) watches.add(w); }
                catch (Exception e) { System.err.println("store skipped a watch: " + e.getMessage()); }
            }
        } catch (IOException e) { throw new IllegalStateException("Could not read " + file, e); }
    }
    private void persist() {
        List<String> userJson = new ArrayList<>(); for (User u : users) userJson.add(u.storedJson());
        List<String> sessionJson = new ArrayList<>(); for (Session s : sessions) sessionJson.add(s.storedJson());
        List<String> watchJson = new ArrayList<>(); for (Watch w : watches) watchJson.add(w.storedJson());
        String json = Json.obj("users", Json.arr(userJson), "sessions", Json.arr(sessionJson), "watches", Json.arr(watchJson));
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
            loadedAt = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) { throw new IllegalStateException("Could not write " + file, e); }
    }
    private static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isBlank() || (hex.length() & 1) != 0) return new byte[0];
        return java.util.HexFormat.of().parseHex(hex);
    }
}
