package com.riftbound.api;

final class Json {
    private Json() {}
    static String str(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
    static String num(int n) { return Integer.toString(n); }
    static String num(double n) { return Double.isFinite(n) ? Double.toString(n) : "null"; }
    static String bool(boolean b) { return b ? "true" : "false"; }
    static String nil() { return "null"; }
    static String obj(String... kv) {
        if ((kv.length & 1) != 0) throw new IllegalArgumentException("obj needs even key/value count");
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append(str(kv[i])).append(':').append(kv[i + 1]);
        }
        sb.append('}');
        return sb.toString();
    }
    static String arr(Iterable<String> encodedItems) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (String item : encodedItems) {
            if (!first) sb.append(',');
            first = false;
            sb.append(item);
        }
        sb.append(']');
        return sb.toString();
    }
}
