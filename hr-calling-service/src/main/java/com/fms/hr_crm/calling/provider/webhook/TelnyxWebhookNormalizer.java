package com.fms.hr_crm.calling.provider.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fms.hr_crm.calling.config.TelnyxProperties;
import com.fms.hr_crm.calling.model.entity.CallStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * {@link WebhookNormalizer} for Telnyx webhooks.
 *
 * <p>Telnyx sends JSON-encoded POST requests (not form-encoded).
 * All methods that parse request data read from the input stream.
 *
 * <p>Telnyx uses TeXML for voice responses, which is structurally identical to TwiML,
 * so the same XML used for Twilio/Plivo works here.
 *
 * <p><b>Signature validation:</b> Telnyx uses Ed25519 asymmetric signatures.
 * The current implementation validates the {@code Telnyx-Timestamp} header to reject
 * replays, and skips full Ed25519 verification. To enable full validation in production:
 * <ol>
 *   <li>Get your webhook signing public key from the Telnyx portal</li>
 *   <li>Add it to {@code telnyx.webhook-public-key} in env</li>
 *   <li>Implement verification using Java's built-in {@code EdDSA} (Java 15+)</li>
 * </ol>
 *
 * <p>Webhook event reference:
 * <a href="https://developers.telnyx.com/api/call-control/call-events">Telnyx Call Events</a>
 */
@Slf4j
public class TelnyxWebhookNormalizer implements WebhookNormalizer {

    /** Reject webhooks with a timestamp older than this (replay attack prevention). */
    private static final long MAX_TIMESTAMP_AGE_SECONDS = 300;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TelnyxProperties props;

    public TelnyxWebhookNormalizer(TelnyxProperties props) {
        this.props = props;
    }

    // ── Signature validation ──────────────────────────────────────────────────

    /**
     * Validates the {@code Telnyx-Timestamp} header to prevent replay attacks.
     *
     * <p>TODO: implement full Ed25519 signature verification for production:
     * <pre>
     *   String tsHeader  = request.getHeader("Telnyx-Timestamp");
     *   String sigHeader = request.getHeader("Telnyx-Signature-Ed25519");
     *   // verify using java.security.Signature with EdDSA + telnyx.webhook-public-key
     * </pre>
     */
    @Override
    public boolean validateSignature(HttpServletRequest request) {
        var apiKey = props.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("[Telnyx] Signature validation skipped — credentials not set");
            return true;
        }

        var tsHeader = request.getHeader("Telnyx-Timestamp");
        if (tsHeader == null) {
            log.warn("[Telnyx] Missing Telnyx-Timestamp header");
            return false;
        }

        try {
            var ts  = Long.parseLong(tsHeader);
            var now = System.currentTimeMillis() / 1000L;
            if (Math.abs(now - ts) > MAX_TIMESTAMP_AGE_SECONDS) {
                log.warn("[Telnyx] Webhook timestamp too old — possible replay attack (ts={} now={})", ts, now);
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("[Telnyx] Invalid Telnyx-Timestamp header: {}", tsHeader);
            return false;
        }

        // TODO: verify Ed25519 signature using telnyx.webhook-public-key
        log.debug("[Telnyx] Timestamp validated; full Ed25519 sig check not yet implemented");
        return true;
    }

    // ── Status callback ───────────────────────────────────────────────────────

    /**
     * Telnyx sends JSON. Event types relevant for status tracking:
     * {@code call.initiated, call.answered, call.hangup, call.recording.saved}.
     *
     * <p>JSON shape:
     * <pre>{@code
     * {
     *   "data": {
     *     "event_type": "call.hangup",
     *     "payload": {
     *       "call_control_id": "v2:T02lltnNwA...",
     *       "call_duration_secs": "35",
     *       "hangup_cause": "normal_clearing"
     *     }
     *   }
     * }
     * }</pre>
     */
    @Override
    public WebhookEvent normalizeStatusCallback(HttpServletRequest request) {
        var root = parseBody(request);
        if (root == null) return null;

        var data      = root.path("data");
        var eventType = data.path("event_type").asText();
        var payload   = data.path("payload");
        var callId    = payload.path("call_control_id").asText(null);

        var status   = fromTelnyxEvent(eventType);
        var duration = parseDuration(payload.path("call_duration_secs").asText(null));

        log.debug("[Telnyx] Status callback — event={} callId={} status={} duration={}s",
                eventType, callId, status, duration);
        return new WebhookEvent(callId, status, duration);
    }

    // ── Voice response ────────────────────────────────────────────────────────

    /**
     * Returns TeXML — Telnyx's TwiML-compatible XML dialect.
     * Structurally identical to TwiML for the Dial/Number case.
     */
    @Override
    public WebhookResponse generateVoiceResponse(String targetNumber, String callerId, boolean record) {
        var xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                  <Dial callerId="%s">
                    <Number>%s</Number>
                  </Dial>
                </Response>""".formatted(callerId, targetNumber);
        return new WebhookResponse(xml, MediaType.TEXT_XML_VALUE);
    }

    // ── Recording callback ────────────────────────────────────────────────────

    /**
     * Telnyx recording event payload:
     * {@code data.payload.recording_urls.mp3}.
     */
    @Override
    public String extractRecordingUrl(HttpServletRequest request) {
        var root = parseBody(request);
        if (root == null) return null;
        var mp3 = root.path("data").path("payload")
                .path("recording_urls").path("mp3").asText(null);
        return (mp3 != null && !mp3.isBlank()) ? mp3 : null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JsonNode parseBody(HttpServletRequest request) {
        try {
            var body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readTree(body);
        } catch (IOException e) {
            log.error("[Telnyx] Failed to parse webhook body: {}", e.getMessage());
            return null;
        }
    }

    private static CallStatus fromTelnyxEvent(String eventType) {
        if (eventType == null) return CallStatus.FAILED;
        return switch (eventType) {
            case "call.initiated"       -> CallStatus.INITIATED;
            case "call.ringing"         -> CallStatus.RINGING;
            case "call.answered"        -> CallStatus.IN_PROGRESS;
            case "call.hangup"          -> CallStatus.COMPLETED;
            case "call.recording.saved" -> CallStatus.COMPLETED;
            default                     -> CallStatus.FAILED;
        };
    }

    private static Integer parseDuration(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return null; }
    }
}