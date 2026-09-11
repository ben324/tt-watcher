package com.riftbound.api;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class Mailer {
    private final String host;
    private final int port;
    private final String user;
    private final String pass;
    private final String from;
    private final boolean startTls;
    Mailer(String host, int port, String user, String pass, String from, boolean startTls) {
        this.host = host; this.port = port;
        this.user = user == null ? "" : user;
        this.pass = pass == null ? "" : pass;
        this.from = from; this.startTls = startTls;
    }
    static Mailer fromEnv() {
        String host = env("SMTP_HOST", "");
        String from = env("MAIL_FROM", env("SMTP_FROM", ""));
        if (host.isBlank() || from.isBlank()) return null;
        int port = 587;
        try { port = Integer.parseInt(env("SMTP_PORT", "587")); } catch (NumberFormatException ignored) {}
        boolean tls = !"false".equalsIgnoreCase(env("SMTP_STARTTLS", port == 587 ? "true" : "false"));
        return new Mailer(host, port, env("SMTP_USER", ""), env("SMTP_PASS", ""), from, tls);
    }
    void send(String to, String subject, String text) throws IOException {
        SSLSocketFactory ssl = (SSLSocketFactory) SSLSocketFactory.getDefault();
        Socket raw = port == 465 ? ssl.createSocket(host, port) : new Socket(host, port);
        raw.setSoTimeout(20000);
        try {
            Session s = new Session(raw);
            s.expect(220);
            s.cmd("EHLO ttwatcher.com", 250);
            if (startTls && port != 465) {
                s.cmd("STARTTLS", 220);
                SSLSocket tls = (SSLSocket) ssl.createSocket(raw, host, port, true);
                tls.startHandshake();
                s = new Session(tls);
                s.cmd("EHLO ttwatcher.com", 250);
            }
            if (!user.isBlank()) {
                s.cmd("AUTH LOGIN", 334);
                s.cmd(b64(user), 334);
                s.cmd(b64(pass), 235);
            }
            s.cmd("MAIL FROM:<" + from + ">", 250);
            s.cmd("RCPT TO:<" + to + ">", 250);
            s.cmd("DATA", 354);
            s.raw("From: " + from + "\r\nTo: " + to + "\r\nSubject: " + subject
                    + "\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n"
                    + text.replace("\r\n.", "\r\n..") + "\r\n.\r\n");
            s.expect(250);
            s.cmd("QUIT", 221);
        } finally { raw.close(); }
    }
    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v.trim();
    }
    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
    private static final class Session {
        final BufferedReader in; final PrintWriter out;
        Session(Socket sock) throws IOException {
            in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8), true);
        }
        void raw(String s) { out.print(s); out.flush(); }
        void cmd(String line, int expect) throws IOException { out.print(line + "\r\n"); out.flush(); expect(expect); }
        void expect(int code) throws IOException {
            String line; String last = "";
            while ((line = in.readLine()) != null) {
                last = line;
                if (line.length() >= 4 && line.charAt(3) == ' ') break;
            }
            if (last.length() < 3 || !last.startsWith(Integer.toString(code))) {
                throw new IOException("SMTP expected " + code + " got " + last);
            }
        }
    }
}
