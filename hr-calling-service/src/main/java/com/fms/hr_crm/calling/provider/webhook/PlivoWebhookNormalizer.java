package com.fms.hr_crm.calling.provider.webhook;

import com.fms.hr_crm.calling.config.PlivoProperties;
import com.fms.hr_crm.calling.model.entity.CallStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@link WebhookNormalizer} for Plivo webhooks.
 *
 * <p>Plivo sends form-encoded POST requests. Signature validation uses HMAC-SHA256
 * of {@code nonce + url + sorted-params}, sent in {@code X-Plivo-Signature-V2}.
 * The nonce comes from {@code X-Plivo-Signature-V2-Nonce}.
 *
 * <p>Voice response uses Plivo Markup Language (PHML), which is structurally
 * identical to TwiML for the {@code <Dial>/<Number>} case.
 *
 * <p>Plivo webhook param reference:
 * <a href="https://www.plivo.com/docs/voice/api/call/#call-status-values">Plivo Call Status</a>
 */
@Slf4j
public class PlivoWebhookNormalizer implements WebhookNormalizer {

    private final PlivoProperties props;

    public PlivoWebhookNormalizer(PlivoProperties props) {
        this.props = props;
    }

    // ── Signature validation ──────────────────────────────────────────────────

    @Override
    public boolean validateSignature(HttpServletRequest request) {
        var authToken = props.authToken();

        if (authToken == null || authToken.isBlank()) {
            log.debug("[Plivo] Signature validation skipped — credentials not set");
            return true;
        }

        var signature = request.getHeader("X-Plivo-Signature-V2");
        var nonce     = request.getHeader("X-Plivo-Signature-V2-Nonce");

        if (signature == null || nonce == null) {
            log.warn("[Plivo] Missing signature headers (X-Plivo-Signature-V2 / Nonce)");
            return false;
        }

        try {
            // Sort POST params alphabetically and concatenate as key=value pairs
            var sortedParams = new TreeMap<>(request.getParameterMap());
            var paramString  = sortedParams.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue()[0])
                    .reduce("", String::concat);

            var url  = reconstructUrl(request);
            var data = nonce + url + paramString;

            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var computed = Base64.getEncoder()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));

            var valid = computed.equals(signature);
            if (!valid) log.warn("[Plivo] Signature mismatch for URL: {}", url);
            return valid;
        } catch (Exception e) {
            log.error("[Plivo] Signature validation error: {}", e.getMessage());
            return false;
        }
    }

    // ── Status callback ───────────────────────────────────────────────────────

    /**
     * Plivo status params: {@code Status}, {@code Duration}, {@code CallUUID}, {@code HangupCause}.
     * Status values: {@code initiated, ringing, answered, busy, failed, no-answer, completed}.
     */
    @Override
    public WebhookEvent normalizeStatusCallback(HttpServletRequest request) {
        var plivoStatus = request.getParameter("Status");
        var callUuid    = request.getParameter("CallUUID");
        var durationStr = request.getParameter("Duration");

        Integer duration = null;
        if (durationStr != null && !durationStr.isBlank()) {
            try { duration = Integer.parseInt(durationStr); } catch (NumberFormatException ignored) {}
        }

        var status = fromPlivoStatus(plivoStatus);
        log.debug("[Plivo] Status callback — callUuid={} status={} duration={}s",
                callUuid, status, duration);
        return new WebhookEvent(callUuid, status, duration);
    }

    // ── Voice response ────────────────────────────────────────────────────────

    /**
     * Returns PHML XML. Plivo PHML is structurally equivalent to TwiML for Dial/Number.
     * {@code record} attribute accepts {@code "true"/"false"} (unlike Twilio enum).
     */
    @Override
    public WebhookResponse generateVoiceResponse(String targetNumber, String callerId, boolean record) {
        var xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                  <Dial callerId="%s" record="%s">
                    <Number>%s</Number>
                  </Dial>
                </Response>""".formatted(callerId, record, targetNumber);
        return new WebhookResponse(xml, MediaType.TEXT_XML_VALUE);
    }

    // ── Recording callback ────────────────────────────────────────────────────

    /**
     * Plivo recording params: {@code RecordUrl}, {@code RecordingID}.
     */
    @Override
    public String extractRecordingUrl(HttpServletRequest request) {
        var url = request.getParameter("RecordUrl");
        return (url != null && !url.isBlank()) ? url : null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static CallStatus fromPlivoStatus(String status) {
        if (status == null) return CallStatus.FAILED;
        return switch (status.toLowerCase()) {
            case "initiated"  -> CallStatus.INITIATED;
            case "ringing"    -> CallStatus.RINGING;
            case "answered"   -> CallStatus.IN_PROGRESS;
            case "completed"  -> CallStatus.COMPLETED;
            case "busy"       -> CallStatus.BUSY;
            case "failed"     -> CallStatus.FAILED;
            case "no-answer"  -> CallStatus.NO_ANSWER;
            case "canceled"   -> CallStatus.CANCELED;
            default           -> CallStatus.FAILED;
        };
    }

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

    /** Unused but kept as a reference for PHML error responses. */
    @SuppressWarnings("unused")
    private static Map<String, String> errorPhml(String message) {
        // Plivo supports <Speak> instead of <Say>:
        // <?xml version="1.0" encoding="UTF-8"?><Response><Speak>message</Speak></Response>
        return Map.of("body",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Speak>"
                        + message + "</Speak></Response>",
                "contentType", MediaType.TEXT_XML_VALUE);
    }
}