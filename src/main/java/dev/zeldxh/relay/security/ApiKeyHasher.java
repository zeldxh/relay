package dev.zeldxh.relay.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

@Component
public class ApiKeyHasher {

    private static final String PREFIX = "relay_";
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return PREFIX + HexFormat.of().formatHex(bytes);
    }

    public String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
