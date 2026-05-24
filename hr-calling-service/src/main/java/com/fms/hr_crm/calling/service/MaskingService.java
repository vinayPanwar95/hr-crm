package com.fms.hr_crm.calling.service;

import com.fms.hr_crm.calling.model.entity.CallSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * The ONLY class in the calling service allowed to read or write
 * {@code CallSession.realNumberEncrypted}. This is a hard security boundary.
 *
 * <p>Uses AES-256-GCM with a random IV per encryption. The stored value is
 * Base64(IV || ciphertext+tag).
 *
 * <p>NEVER log, print, or expose the plaintext phone number that passes through here.
 */
@Service
@Slf4j
public class MaskingService {

    private static final String ALGORITHM       = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LENGTH   = 12;  // 96 bits
    private static final int    GCM_TAG_BITS    = 128;

    private final SecretKey secretKey;
    private final SecureRandom random = new SecureRandom();

    public MaskingService(SecretKey encryptionKey) {
        this.secretKey = encryptionKey;
    }

    /**
     * Encrypts the lead's real phone number and stores it on the session.
     * This is the ONLY method that writes {@code realNumberEncrypted}.
     *
     * <p>Do NOT log the {@code realPhone} parameter.
     */
    public void encryptAndStore(CallSession session, String realPhone) {
        session.setRealNumberEncrypted(encrypt(realPhone));
        log.debug("Encrypted real number for session {}", session.getId());
    }

    /**
     * Decrypts the real phone number from the session for Twilio use.
     * Should ONLY be called from {@link com.fms.hr_crm.calling.controller.WebhookController}.
     *
     * <p>Do NOT log the return value of this method.
     */
    public String decryptForTwilio(CallSession session) {
        log.debug("Decrypting real number for session {} (Twilio use only)", session.getId());
        return decrypt(session.getRealNumberEncrypted());
    }

    /**
     * Decrypts the real phone number for outbound AI campaign dialing.
     *
     * <p>AI campaigns dial leads directly — unlike the recruiter flow where the phone
     * is passed only via TwiML response, here we must pass it to the provider REST API.
     * This is an intentional and auditable security boundary.
     *
     * <p>Should ONLY be called from
     * {@link com.fms.hr_crm.calling.service.CallService#initiateAiCampaignCall}.
     * Do NOT log the return value.
     */
    public String decryptForCampaignDial(CallSession session) {
        log.debug("Decrypting real number for AI campaign dial — session {}", session.getId());
        return decrypt(session.getRealNumberEncrypted());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String encrypt(String plainText) {
        try {
            var iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);

            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            var cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            var combined = new byte[GCM_IV_LENGTH + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherBytes, 0, combined, GCM_IV_LENGTH, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt phone number", e);
        }
    }

    private String decrypt(String encoded) {
        try {
            var combined    = Base64.getDecoder().decode(encoded);
            var iv          = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            var cipherBytes = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt phone number", e);
        }
    }
}