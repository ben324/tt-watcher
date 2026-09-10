package com.riftbound.api;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;

final class Passwords {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RNG = new SecureRandom();
    private Passwords() {}
    static byte[] salt() { byte[] salt = new byte[16]; RNG.nextBytes(salt); return salt; }
    static byte[] tokenBytes() { byte[] token = new byte[24]; RNG.nextBytes(token); return token; }
    static String hex(byte[] raw) { return HexFormat.of().formatHex(raw); }
    static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        } finally { spec.clearPassword(); }
    }
    static boolean verify(char[] password, byte[] salt, byte[] expected) {
        byte[] actual = hash(password, salt);
        try { return Arrays.equals(actual, expected); }
        finally { Arrays.fill(actual, (byte) 0); }
    }
}
