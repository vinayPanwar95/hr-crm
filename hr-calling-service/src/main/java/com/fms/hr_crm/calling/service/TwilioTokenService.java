package com.fms.hr_crm.calling.service;

import com.fms.hr_crm.calling.config.TwilioProperties;
import com.fms.hr_crm.calling.model.dto.TwilioTokenResponse;
import com.twilio.jwt.accesstoken.AccessToken;
import com.twilio.jwt.accesstoken.VoiceGrant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Generates Twilio Access Tokens for browser-based calling via Twilio Client SDK.
 *
 * <p>Requires {@code TWILIO_API_KEY_SID}, {@code TWILIO_API_KEY_SECRET}, and
 * {@code TWILIO_TWIML_APP_SID} to be configured. Returns a stub token if unconfigured.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioTokenService {

    private final TwilioProperties props;

    /** Generates a short-lived Access Token (1 hour) for the given recruiter identity. */
    public TwilioTokenResponse generateAccessToken(UUID recruiterId, UUID tenantId) {
        var identity = "recruiter-" + recruiterId + "-tenant-" + tenantId;

        if (!isConfigured()) {
            log.warn("Twilio API Key or TwiML App SID not configured — returning stub token");
            return new TwilioTokenResponse("stub-token-configure-twilio-api-key", identity);
        }

        var grant = new VoiceGrant();
        grant.setOutgoingApplicationSid(props.twimlAppSid());
        grant.setIncomingAllow(true);

        try {
            var token = new AccessToken.Builder(
                    props.accountSid(),
                    props.apiKeySid(),
                    props.apiKeySecret()
            )
            .identity(identity)
            .grant(grant)
            .ttl(3600)
            .build();

            log.debug("Generated Twilio access token for identity={}", identity);
            return new TwilioTokenResponse(token.toJwt(), identity);
        } catch (Exception e) {
            log.warn("Failed to generate Twilio access token (check TWILIO_API_KEY_SECRET length — must be >= 32 chars): {}",
                    e.getMessage());
            return new TwilioTokenResponse("stub-token-api-key-secret-too-short", identity);
        }
    }

    private boolean isConfigured() {
        return props.apiKeySid() != null && !props.apiKeySid().isBlank()
                && props.apiKeySecret() != null && !props.apiKeySecret().isBlank()
                && props.twimlAppSid() != null && !props.twimlAppSid().isBlank();
    }
}