package com.fms.hr_crm.calling.controller;

import com.fms.hr_crm.calling.config.TwilioProperties;
import com.fms.hr_crm.calling.model.entity.CallStatus;
import com.fms.hr_crm.calling.repository.CallSessionRepository;
import com.fms.hr_crm.calling.service.CallService;
import com.fms.hr_crm.calling.service.MaskingService;
import com.twilio.security.RequestValidator;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Dial;
import com.twilio.twiml.voice.Number;
import com.twilio.twiml.voice.Say;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @deprecated Replaced by {@link WebhookController} — the provider-agnostic webhook handler
 *             introduced as part of the multi-provider abstraction (CallingProvider interface).
 *
 * <p>This class is DISABLED ({@code @RestController} commented out) to prevent conflicting
 * route mappings with {@link WebhookController} on {@code /webhook/call/**}.
 * All logic has been migrated to:
 * <ul>
 *   <li>{@link com.fms.hr_crm.calling.provider.webhook.TwilioWebhookNormalizer} — signature validation,
 *       status parsing, TwiML generation</li>
 *   <li>{@link WebhookController} — the unified webhook handler</li>
 * </ul>
 * To re-enable (e.g. for debugging), restore the {@code @RestController} annotation
 * and comment out {@code WebhookController}.
 */
// @RestController  ← DISABLED: replaced by WebhookController
@Deprecated
@RequestMapping("/webhook/call")
@RequiredArgsConstructor
@Slf4j
public class TwilioWebhookController {

    private final CallSessionRepository repo;
    private final MaskingService        maskingService;
    private final CallService           callService;
    private final TwilioProperties      props;

    /**
     * Voice webhook — called by Twilio when the recruiter answers their phone.
     * Returns TwiML instructing Twilio to dial the lead's real number.
     *
     * <p>THIS IS THE ONLY PLACE where {@code decryptForTwilio()} is called.
     */
    @PostMapping(value = "/voice/{sessionId}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> voice(
            @PathVariable UUID sessionId,
            HttpServletRequest request,
            @RequestParam Map<String, String> params) {

        if (!validateSignature(request, params)) {
            log.warn("Invalid Twilio signature on /voice/{}", sessionId);
            return ResponseEntity.status(403).contentType(MediaType.APPLICATION_XML)
                    .body(forbiddenTwiml());
        }

        var session = repo.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("Voice webhook for unknown session {}", sessionId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(errorTwiml("Session not found"));
        }

        // decryptForTwilio() is ONLY called here — do NOT log the returned value
        String realPhone = maskingService.decryptForTwilio(session);

        var twiml = new VoiceResponse.Builder()
                .dial(new Dial.Builder()
                        .callerId(props.fromNumber())
                        .record(Dial.Record.RECORD_FROM_ANSWER)
                        .number(new Number.Builder(realPhone).build())
                        .build())
                .build();

        log.info("Voice TwiML generated for session {}", sessionId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(twiml.toXml());
    }

    /**
     * TwiML App voice webhook — called by Twilio Client SDK (browser-based calling).
     * The browser passes {@code leadId} and {@code tenantId} as custom params.
     */
    @PostMapping(value = "/browser", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> browserVoice(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {

        if (!validateSignature(request, params)) {
            log.warn("Invalid Twilio signature on /browser webhook");
            return ResponseEntity.status(403).contentType(MediaType.APPLICATION_XML)
                    .body(forbiddenTwiml());
        }

        var leadIdStr   = params.get("leadId");
        var tenantIdStr = params.get("tenantId");

        if (leadIdStr == null || tenantIdStr == null) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML)
                    .body(errorTwiml("Missing leadId or tenantId parameter"));
        }

        // For browser calls, look up the most recent session for this lead
        var sessions = repo.findByTenantIdAndLeadId(
                UUID.fromString(tenantIdStr), UUID.fromString(leadIdStr),
                org.springframework.data.domain.PageRequest.of(0, 1,
                        org.springframework.data.domain.Sort.by("startedAt").descending()));

        if (sessions.isEmpty()) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML)
                    .body(errorTwiml("No active session found for lead"));
        }

        var session  = sessions.getContent().get(0);
        // decryptForTwilio() is ONLY called here — do NOT log the returned value
        var realPhone = maskingService.decryptForTwilio(session);

        var twiml = new VoiceResponse.Builder()
                .dial(new Dial.Builder()
                        .callerId(props.fromNumber())
                        .record(Dial.Record.RECORD_FROM_ANSWER)
                        .number(new Number.Builder(realPhone).build())
                        .build())
                .build();

        log.info("Browser voice TwiML generated for session {}", session.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(twiml.toXml());
    }

    /**
     * Status callback — called by Twilio when call status changes.
     * Updates the call session status and duration.
     */
    @PostMapping(value = "/status/{sessionId}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> statusCallback(
            @PathVariable UUID sessionId,
            @RequestParam(value = "CallStatus",   defaultValue = "") String twilioStatus,
            @RequestParam(value = "CallDuration",  required = false) Integer duration,
            HttpServletRequest request,
            @RequestParam Map<String, String> params) {

        if (!validateSignature(request, params)) {
            log.warn("Invalid Twilio signature on /status/{}", sessionId);
            return ResponseEntity.status(403).build();
        }

        var status = CallStatus.fromTwilio(twilioStatus);
        callService.updateStatus(sessionId, status, duration);

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body("<Response/>");
    }

    /**
     * Recording callback — called by Twilio when a recording is available.
     */
    @PostMapping(value = "/recording/{sessionId}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> recordingCallback(
            @PathVariable UUID sessionId,
            @RequestParam(value = "RecordingUrl", required = false) String recordingUrl,
            HttpServletRequest request,
            @RequestParam Map<String, String> params) {

        if (!validateSignature(request, params)) {
            log.warn("Invalid Twilio signature on /recording/{}", sessionId);
            return ResponseEntity.status(403).build();
        }

        if (recordingUrl != null && !recordingUrl.isBlank()) {
            callService.saveRecording(sessionId, recordingUrl + ".mp3");
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body("<Response/>");
    }

    // ── Signature validation ──────────────────────────────────────────────────

    private boolean validateSignature(HttpServletRequest request, Map<String, String> params) {
        var authToken = props.authToken();
        if (authToken == null || authToken.isBlank() || "test_token".equals(authToken)) {
            // Skip validation in local dev with placeholder credentials
            log.debug("Twilio signature validation skipped (local dev)");
            return true;
        }

        try {
            var validator = new RequestValidator(authToken);
            var url       = reconstructUrl(request);
            var signature = request.getHeader("X-Twilio-Signature");

            if (signature == null) {
                log.warn("Missing X-Twilio-Signature header");
                return false;
            }

            // Build a mutable copy for the validator (it needs a Map<String, String>)
            var paramsCopy = new HashMap<>(params);
            return validator.validate(url, paramsCopy, signature);
        } catch (Exception e) {
            log.error("Twilio signature validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Reconstructs the full public-facing URL that Twilio signed.
     *
     * <p>When running behind ngrok or any reverse proxy, the server receives
     * HTTP on localhost while Twilio signed the HTTPS public URL. This method
     * reads {@code X-Forwarded-Proto} and {@code X-Forwarded-Host} (set by ngrok)
     * to rebuild the correct URL. If those headers are absent (direct connection),
     * it falls back to {@code request.getRequestURL()} which is already correct.
     *
     * <p>{@code server.forward-headers-strategy=native} in application-local.yaml
     * makes Spring rewrite the URL automatically, so the forwarded-header branch
     * below is a safety net for environments where that property is not set.
     */
    private String reconstructUrl(HttpServletRequest request) {
        var forwardedProto = request.getHeader("X-Forwarded-Proto");
        var forwardedHost  = request.getHeader("X-Forwarded-Host");

        String base;
        if (forwardedProto != null && !forwardedProto.isBlank()
                && forwardedHost != null && !forwardedHost.isBlank()) {
            // Behind ngrok / load balancer — use the public-facing URL
            base = forwardedProto + "://" + forwardedHost + request.getRequestURI();
            log.debug("reconstructUrl() — using forwarded headers: {}", base);
        } else {
            base = request.getRequestURL().toString();
            log.debug("reconstructUrl() — using direct URL: {}", base);
        }

        var query = request.getQueryString();
        return query != null ? base + "?" + query : base;
    }

    private String forbiddenTwiml() {
        return new VoiceResponse.Builder()
                .say(new Say.Builder("This request could not be authenticated.").build())
                .build().toXml();
    }

    private String errorTwiml(String message) {
        return new VoiceResponse.Builder()
                .say(new Say.Builder("Sorry, " + message + ". Please try again.").build())
                .build().toXml();
    }
}