package com.anuraggupta.sqlgenie.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * SHA-256 is used (not BCrypt) because refresh tokens are already high-entropy
 * random values, not low-entropy passwords - a fast hash is correct here since
 * we need to look up a token by its hash, and BCrypt is deliberately too slow for that.
 */
public final class TokenHashUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenHashUtil() {
    }

    public static String generateRawToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
