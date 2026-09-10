package com.riftbound.api;

final class User {
    final String id, email, displayName, passwordSaltHex, passwordHashHex, createdAt;
    User(String id, String email, String displayName, String passwordSaltHex, String passwordHashHex, String createdAt) {
        this.id = id; this.email = email; this.displayName = displayName;
        this.passwordSaltHex = passwordSaltHex; this.passwordHashHex = passwordHashHex; this.createdAt = createdAt;
    }
    String publicJson() {
        return Json.obj("id", Json.str(id), "email", Json.str(email), "displayName", Json.str(displayName), "createdAt", Json.str(createdAt));
    }
    String storedJson() {
        return Json.obj("id", Json.str(id), "email", Json.str(email), "displayName", Json.str(displayName), "passwordSalt", Json.str(passwordSaltHex), "passwordHash", Json.str(passwordHashHex), "createdAt", Json.str(createdAt));
    }
    static User fromJson(String obj) {
        return new User(Body.strOrEmpty(obj, "id"), Body.strOrEmpty(obj, "email"), Body.strOrEmpty(obj, "displayName"), Body.strOrEmpty(obj, "passwordSalt"), Body.strOrEmpty(obj, "passwordHash"), Body.strOrEmpty(obj, "createdAt"));
    }
}
