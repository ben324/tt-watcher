package com.riftbound.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

final class AuthHandler implements HttpHandler {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final App app;
    AuthHandler(App app) { this.app = app; }
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Http.preflight(ex)) return;
        String path = Http.path(ex);
        String method = ex.getRequestMethod().toUpperCase(Locale.ROOT);
        try {
            if (("/api/auth/register".equals(path) || "/api/auth/signup".equals(path)) && "POST".equals(method)) { register(ex); return; }
            if ("/api/auth/login".equals(path) && "POST".equals(method)) { login(ex); return; }
            if ("/api/auth/logout".equals(path) && "POST".equals(method)) { logout(ex); return; }
            if (("/api/auth/me".equals(path) || "/api/me".equals(path)) && "GET".equals(method)) { me(ex); return; }
            if (("/api/auth/me".equals(path) || "/api/me".equals(path)) && "PATCH".equals(method)) { patchMe(ex); return; }
            if (path.startsWith("/api/auth") && "POST".equals(method)) {
                Http.json(ex, 404, Json.obj("error", Json.str("not_found"), "message", Json.str("Use POST /api/auth/register, /api/auth/login, /api/auth/logout")));
                return;
            }
            Http.methodNotAllowed(ex, "GET, POST, PATCH, OPTIONS");
        } catch (IllegalArgumentException e) {
            Http.json(ex, 400, Json.obj("error", Json.str("bad_request"), "message", Json.str(e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            Http.json(ex, 500, Json.obj("error", Json.str("internal"), "message", Json.str("Auth request failed")));
        }
    }
    private void register(HttpExchange ex) throws IOException {
        String body = Http.readBody(ex);
        String email = Body.str(body, "email");
        String password = Body.str(body, "password");
        validateCredentials(email, password);
        char[] chars = password.toCharArray();
        try {
            User user = app.store.createUser(email, chars);
            Session session = app.store.createSession(user.id);
            Http.setSessionCookie(ex, session.token);
            Http.json(ex, 201, sessionPayload(user, session));
        } catch (IllegalStateException e) {
            if ("email_taken".equals(e.getMessage())) {
                Http.json(ex, 409, Json.obj("error", Json.str("email_taken"), "message", Json.str("An account already exists for that email")));
                return;
            }
            throw e;
        } finally { Arrays.fill(chars, '\0'); }
    }
    private void login(HttpExchange ex) throws IOException {
        String body = Http.readBody(ex);
        String email = Body.str(body, "email");
        String password = Body.str(body, "password");
        if (email == null || password == null) throw new IllegalArgumentException("email and password are required");
        char[] chars = password.toCharArray();
        try {
            Optional<User> user = app.store.authenticate(email, chars);
            if (user.isEmpty()) {
                Http.json(ex, 401, Json.obj("error", Json.str("invalid_credentials"), "message", Json.str("Email or password is wrong")));
                return;
            }
            Session session = app.store.createSession(user.get().id);
            Http.setSessionCookie(ex, session.token);
            Http.json(ex, 200, sessionPayload(user.get(), session));
        } finally { Arrays.fill(chars, '\0'); }
    }
    private void logout(HttpExchange ex) throws IOException {
        app.store.deleteSession(Http.bearer(ex));
        Http.clearSessionCookie(ex);
        Http.json(ex, 200, Json.obj("ok", Json.bool(true), "message", Json.str("Logged out")));
    }
    private void me(HttpExchange ex) throws IOException {
        Optional<User> user = app.requireUser(ex);
        if (user.isEmpty()) return;
        Http.json(ex, 200, Json.obj("user", user.get().publicJson()));
    }
    private void patchMe(HttpExchange ex) throws IOException {
        Optional<User> found = app.requireUser(ex);
        if (found.isEmpty()) return;
        User current = found.get();
        String body = Http.readBody(ex);
        String phone = current.phone;
        boolean notifyEmail = current.notifyEmail;
        boolean notifySms = current.notifySms;
        if (Body.has(body, "phone")) phone = User.normalizePhone(Body.strOrEmpty(body, "phone"));
        if (Body.has(body, "notifyEmail")) notifyEmail = Body.bool(body, "notifyEmail");
        if (Body.has(body, "notifySms")) notifySms = Body.bool(body, "notifySms");
        if (notifySms && (phone == null || phone.isBlank())) throw new IllegalArgumentException("phone is required when notifySms is true");
        User updated = current.withContact(phone, notifyEmail, notifySms);
        app.store.replaceUser(updated);
        Http.json(ex, 200, Json.obj("user", updated.publicJson()));
    }
    private static void validateCredentials(String email, String password) {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) throw new IllegalArgumentException("email looks invalid");
        if (password == null || password.length() < 8) throw new IllegalArgumentException("password must be at least 8 characters");
    }
    private static String sessionPayload(User user, Session session) {
        return Json.obj("token", Json.str(session.token), "expiresAt", Json.str(session.expiresAt), "user", user.publicJson());
    }
}
