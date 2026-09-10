package com.riftbound.events.json;

import java.util.ArrayList;
import java.util.List;

public final class JsonBits {
    private JsonBits() {}

    public static String quoted(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return "";
        i += needle.length();
        i = skipWs(json, i);
        if (i >= json.length()) return "";
        if (json.charAt(i) == 'n' && json.startsWith("null", i)) return "";
        if (json.charAt(i) != '"') return "";
        return readString(json, i + 1);
    }

    public static int number(String json, String key, int fallback) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return fallback;
        i = skipWs(json, i + needle.length());
        int j = i;
        if (j < json.length() && json.charAt(j) == '-') j++;
        while (j < json.length() && Character.isDigit(json.charAt(j))) j++;
        if (j == i || (j == i + 1 && json.charAt(i) == '-')) return fallback;
        try { return Integer.parseInt(json.substring(i, j)); } catch (NumberFormatException e) { return fallback; }
    }

    public static List<String> objectsInArray(String json, String arrayKey) {
        String needle = "\"" + arrayKey + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return List.of();
        i = skipWs(json, i + needle.length());
        if (i >= json.length() || json.charAt(i) != '[') return List.of();
        List<String> out = new ArrayList<>();
        int depth = 0, start = -1;
        boolean inStr = false, escape = false;
        for (int p = i + 1; p < json.length(); p++) {
            char c = json.charAt(p);
            if (inStr) {
                if (escape) escape = false;
                else if (c == '\\') escape = true;
                else if (c == '"') inStr = false;
                continue;
            }
            if (c == '"') { inStr = true; continue; }
            if (c == '{') { if (depth == 0) start = p; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start >= 0) { out.add(json.substring(start, p + 1)); start = -1; } }
            else if (c == ']' && depth == 0) break;
        }
        return out;
    }

    public static String nestedObject(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return "";
        i = skipWs(json, i + needle.length());
        if (i >= json.length() || json.charAt(i) != '{') return "";
        int depth = 0; boolean inStr = false, escape = false;
        for (int p = i; p < json.length(); p++) {
            char c = json.charAt(p);
            if (inStr) {
                if (escape) escape = false;
                else if (c == '\\') escape = true;
                else if (c == '"') inStr = false;
                continue;
            }
            if (c == '"') inStr = true;
            else if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return json.substring(i, p + 1); }
        }
        return "";
    }

    private static int skipWs(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static String readString(String json, int start) {
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                sb.append(switch (c) { case 'n' -> '\n'; case 't' -> '\t'; case 'r' -> '\r'; case '"' -> '"'; case '\\' -> '\\'; default -> c; });
                escape = false;
            } else if (c == '\\') escape = true;
            else if (c == '"') return sb.toString();
            else sb.append(c);
        }
        return sb.toString();
    }
}
