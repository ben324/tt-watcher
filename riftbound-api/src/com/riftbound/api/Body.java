package com.riftbound.api;

import com.riftbound.events.json.JsonBits;

final class Body {
    private Body() {}
    static String str(String json, String key) {
        if (json == null || json.isBlank() || !has(json, key)) return null;
        String v = JsonBits.quoted(json, key);
        return v.isBlank() ? null : v.trim();
    }
    static String strOrEmpty(String json, String key) {
        String v = str(json, key);
        return v == null ? "" : v;
    }
    static Integer integer(String json, String key) {
        if (json == null || !has(json, key)) return null;
        int n = JsonBits.number(json, key, Integer.MIN_VALUE);
        return n == Integer.MIN_VALUE ? null : n;
    }
    static Double decimal(String json, String key) {
        if (json == null || !has(json, key)) return null;
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        i += needle.length();
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        int j = i;
        if (j < json.length() && json.charAt(j) == '-') j++;
        boolean dot = false;
        while (j < json.length()) {
            char c = json.charAt(j);
            if (c == '.' && !dot) { dot = true; j++; }
            else if (Character.isDigit(c)) j++;
            else break;
        }
        if (j == i) return null;
        try { return Double.parseDouble(json.substring(i, j)); }
        catch (NumberFormatException e) { return null; }
    }
    static boolean has(String json, String key) {
        return json != null && json.contains("\"" + key + "\":");
    }
    static boolean bool(String json, String key) {
        if (!has(json, key)) return false;
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return false;
        i += needle.length();
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        return json.startsWith("true", i);
    }
}
