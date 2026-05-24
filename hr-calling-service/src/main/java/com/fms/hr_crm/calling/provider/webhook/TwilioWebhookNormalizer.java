package com.fms.hr_crm.calling.provider.webhook;

import com.fms.hr_crm.calling.config.TwilioProperties;
import com.fms.hr_crm.calling.model.entity.CallStatus;
import com.twilio.security.RequestValidator;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Dial;
import com.twilio.twiml.voice.Number;
import com.twilio.twiml.voice.Say;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.util.HashMap;

/**
 * {@link WebhookNormalizer} for Twilio webhooks.
 *
 * <p>Twilio sends form-encoded POST requests and validates requests using
 * HMAC-SHA1 of the full URL + sorted POST params, sent in {@code X-Twilio-Signature}.
 *
 * <p>Voice response uses the Twilio TwiML SDK for type-safe XML generation.
 */
@Slf4j
public class TwilioWebhookNormalizer implements WebhookNormalizer {

    private final TwilioProperties props;

    public TwilioWebhookNormalizer(TwilioProperties props) {
        this.props = props;
    }

    // ── Signature validation ──────────────────────────────────────────────────

    @Override
    public boolean validateSignature(HttpServletRequest request) {
        var authToken = props.authToken();

        // Skip validation in local dev when credentials are not set
        if (authToken == null || authToken.isBlank() || "ACtest".equals(props.accountSid())) {
            log.debug("[Twilio] Signature validation skipped — placeholder credentials");
            return true;
        }

        var signature = request.getHeader("X-Twilio-Signature");
        if (signature == null) {
            log.warn("[Twilio] Missing X-Twilio-Signature header");
            return false;
        }

        try {
            var validator  = new RequestValidator(authToken);
            var url        = reconstructUrl(request);
            // getParameterMap() returns all form params; must be a mutable HashMap for Twilio
            var paramsCopy = new HashMap<>(request.getParameterMap().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            java.util.Map.Entry::getKey,
                            e -> e.getValue()[0])));
            var valid = validator.validate(url, paramsCopy, signature);
            if (!valid) log.warn("[Twilio] Signature mismatch for URL: {}", url);
            return valid;
        } catch (Exception e) {
            log.error("[Twilio] Signature validation error: {}", e.getMessage());
            return false;
        }
    }

    // ── Status callback ───────────────────────────────────────────────────────

    @Override
    public WebhookEvent normalizeStatusCallback(HttpServletRequest request) {
        var twilioStatus = request.getParameter("CallStatus");
        var callSid      = request.getParameter("CallSid");
        var durationStr  = request.getParameter("CallDuration");

        Integer duration = null;
        if (durationStr != null && !durationStr.isBlank()) {
            try { duration = Integer.parseInt(durationStr); } catch (NumberFormatException ignored) {}
        }

        var status = CallStatus.fromTwilio(twilioStatus);
        log.debug("[Twilio] Status callback — callSid={} status={} duration={}s",
                callSid, status, duration);
        return new WebhookEvent(callSid, status, duration);
    }

    // ── Voice response ────────────────────────────────────────────────────────

    @Override
    public WebhookResponse generateVoiceResponse(String targetNumber, String callerId, boolean record) {
        var dialRecord = record ? Dial.Record.RECORD_FROM_ANSWER : Dial.Record.DO_NOT_RECORD;
        var twiml = new VoiceResponse.Builder()
                .dial(new Dial.Builder()
                        .callerId(callerId)
                        .record(dialRecord)
                        .number(new Number.Builder(targetNumber).build())
                        .build())
                .build();
        return new WebhookResponse(twiml.toXml(), MediaType.TEXT_XML_VALUE);
    }

    // ── Recording callback ────────────────────────────────────────────────────

    @Override
    public String extractRecordingUrl(HttpServletRequest request) {
        var url = request.getParameter("RecordingUrl");
        return (url != null && !url.isBlank()) ? url + ".mp3" : null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Reconstructs the public URL Twilio used when signing the request.
     * Handles ngrok/reverse-proxy by reading {@code X-Forwarded-*} headers.
     */
    private String reconstructUrl(HttpServletRequest request) {
        var proto = request.getHeader("X-Forwarded-Proto");
        var host  = request.getHeader("X-Forwarded-Host");
        if (proto != null && !proto.isBlank() && host != null && !host.isBlank()) {
            var base  = proto + "://" + host + request.getRequestURI();
            var query = request.getQueryString();
            return query != null ? base + "?" + query : base;
        }
        var base  = request.getRequestURL().toString();
        var query = request.getQueryString();
        return query != null ? base + "?" + query : base;
    }

    /** Generates a TwiML error response to speak to the caller. */
    @SuppressWarnings("unused")
    private String errorTwiml(String message) {
        return new VoiceResponse.Builder()
                .say(new Say.Builder("Sorry, " + message).build())
                .build().toXml();
    }
}