package com.fms.hr_crm.calling.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Provides the AES-256 secret key used by {@link com.fms.hr_crm.calling.service.MaskingService}.
 *
 * <p>In production, set {@code ENCRYPTION_KEY} to a Base64-encoded 32-byte key.
 * In local dev, a random key is generated — encrypted data won't survive restarts.
 */
@Configuration
@Slf4j
public class EncryptionConfig {

    @Value("${encryption.key:}")
    private String base64Key;

    @Bean
    public SecretKey encryptionKey() {
        if (base64Key != null && !base64Key.isBlank()) {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            if (keyBytes.length != 32) {
                throw new IllegalStateException(
                        "ENCRYPTION_KEY must decode to exactly 32 bytes (AES-256), got " + keyBytes.length);
            }
            log.info("AES-256 encryption key loaded from environment variable");
            return new SecretKeySpec(keyBytes, "AES");
        }

        // Local dev only — generate a random ephemeral key
        try {
            var keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            var key = keyGen.generateKey();
            log.warn("ENCRYPTION_KEY not set — using ephemeral random AES key (data won't survive restart). " +
                     "Set ENCRYPTION_KEY for production.");
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate ephemeral AES key", e);
        }
    }
}