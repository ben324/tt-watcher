package com.riftbound.api;

final class User {
    final String id;
    final String email;
    final String phone;
    final boolean notifyEmail;
    final boolean notifySms;
    final String passwordSaltHex;
    final String passwordHashHex;
    final String createdAt;

    User(String id, String email, String phone, boolean notifyEmail, boolean notifySms,
            String passwordSaltHex, String passwordHashHex, String createdAt) {
        this.id = id;
        this.email = email;
        this.phone = phone == null ? "" : phone;
        this.notifyEmail = notifyEmail;
        this.notifySms = notifySms;
        this.passwordSaltHex = passwordSaltHex;
        this.passwordHashHex = passwordHashHex;
        this.createdAt = createdAt;
    }

    User withContact(String phone, boolean notifyEmail, boolean notifySms) {
        return new User(id, email, phone, notifyEmail, notifySms, passwordSaltHex, passwordHashHex, createdAt);
    }

    String publicJson() {
        return Json.obj(
                "id", Json.str(id),
                "email", Json.str(email),
                "phone", Json.str(phone),
                "notifyEmail", Json.bool(notifyEmail),
                "notifySms", Json.bool(notifySms),
                "createdAt", Json.str(createdAt));
    }

    String storedJson() {
        return Json.obj(
                "id", Json.str(id),
                "email", Json.str(email),
                "phone", Json.str(phone),
                "notifyEmail", Json.bool(notifyEmail),
                "notifySms", Json.bool(notifySms),
                "passwordSalt", Json.str(passwordSaltHex),
                "passwordHash", Json.str(passwordHashHex),
                "createdAt", Json.str(createdAt));
    }

    static User fromJson(String obj) {
        boolean emailOn = !Body.has(obj, "notifyEmail") || Body.bool(obj, "notifyEmail");
        boolean smsOn = Body.has(obj, "notifySms") && Body.bool(obj, "notifySms");
        return new User(
                Body.strOrEmpty(obj, "id"),
                Body.strOrEmpty(obj, "email"),
                Body.strOrEmpty(obj, "phone"),
                emailOn,
                smsOn,
                Body.strOrEmpty(obj, "passwordSalt"),
                Body.strOrEmpty(obj, "passwordHash"),
                Body.strOrEmpty(obj, "createdAt"));
    }

    static String normalizePhone(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.isBlank()) return "";
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c >= '0' && c <= '9') digits.append(c);
        }
        if (digits.length() == 10) return "+1" + digits;
        if (digits.length() == 11 && digits.charAt(0) == '1') return "+" + digits;
        if (digits.length() >= 10 && digits.length() <= 15 && trimmed.startsWith("+")) return "+" + digits;
        throw new IllegalArgumentException("phone must be E.164 like +15551234567");
    }
}
