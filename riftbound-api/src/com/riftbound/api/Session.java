package com.riftbound.api;

final class Session {
    final String token, userId, createdAt, expiresAt;
    Session(String token, String userId, String createdAt, String expiresAt) {
        this.token = token; this.userId = userId; this.createdAt = createdAt; this.expiresAt = expiresAt;
    }
    String storedJson() {
        return Json.obj("token", Json.str(token), "userId", Json.str(userId), "createdAt", Json.str(createdAt), "expiresAt", Json.str(expiresAt));
    }
    static Session fromJson(String obj) {
        return new Session(Body.strOrEmpty(obj, "token"), Body.strOrEmpty(obj, "userId"), Body.strOrEmpty(obj, "createdAt"), Body.strOrEmpty(obj, "expiresAt"));
    }
}
