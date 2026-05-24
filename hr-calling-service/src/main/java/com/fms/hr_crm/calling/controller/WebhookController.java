package com.fms.hr_crm.calling.controller;

import com.fms.hr_crm.calling.provider.CallingProvider;
import com.fms.hr_crm.calling.provider.webhook.WebhookNormalizer;
import com.fms.hr_crm.calling.repository.CallSessionRepository;
import com.fms.hr_crm.calling.service.CallService;
import com.fms.hr_crm.calling.service.MaskingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Provider-agnostic webhook controller.
 *
 * <p>Handles inbound callbacks from whichever telephony provider is active
 * ({@code calling.provider=twilio|plivo|telnyx}). All provider-specific parsing,
 * signature validation, and response generation is delegated to the injected
 * {@link WebhookNormalizer} — this controller contains zero provider-specific logic.
 *
 * <p><strong>Security rules (unchanged from the original TwilioWebhookController):</strong>
 * <ul>
 *   <li>Every endpoint validates the provider signature — invalid requests get 403.</li>
 *   <li>This is the ONLY controller that calls {@link MaskingService#decryptForTwilio}.</li>
 *   <li>Decrypted phone numbers are passed directly into the response — never logged, never stored.</li>
 * </ul>
 *
 * <p>Replaces {@link TwilioWebhookController} (disabled — see that class for migration note).
 */
@RestController
@RequestMapping("/webhook/call")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final CallSessionRepository repo;
    private final MaskingService        maskingService;
    private final CallService           callService;
    private final WebhookNormalizer     normalizer;
    private final CallingProvider       callingProvider;

    // ── Voice webhook ─────────────────────────────────────────────────────────

    /**
     * Called by the provider when the recruiter answers their phone.
     * Returns TwiML/PHML/TeXML instructing the provider to bridge to the lead.
     *
     * <p>THIS IS THE ONLY PLACE where {@link MaskingService#decryptForTwilio()} is called.
     * The decrypted number flows directly into the response body — it is never stored or logged.
     */
    @PostMapping("/voice/{sessionId}")
    public ResponseEntity<String> voice(
            @PathVariable UUID sessionId,
            HttpServletRequest request) {

        log.info("voice() — sessionId={} provider={}", sessionId, callingProvider.providerName());

        if (!normalizer.validateSignature(request)) {
            log.warn("voice() — invalid signature rejected for session {}", sessionId);
            return ResponseEntity.status(403).build();
        }

        var session = repo.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("voice() — unknown session {}", sessionId);
            return ResponseEntity.status(404).build();
        }

        // decryptForTwilio() is the ONLY caller of MaskingService here — do NOT log the result
        var realPhone = maskingService.decryptForTwilio(session);

        var response = normalizer.generateVoiceResponse(
                realPhone, callingProvider.fromNumber(), true);

        log.info("voice() — voice response generated for session {}", sessionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(response.contentType()))
                .body(response.body());
    }

    // ── Status callback ───────────────────────────────────────────────────────

    /**
     * Called by the provider on every call status change (ringing → answered → completed …).
     * Updates the {@link com.fms.hr_crm.calling.model.entity.CallSession} status and duration.
     */
    @PostMapping("/status/{sessionId}")
    public ResponseEntity<Void> statusCallback(
            @PathVariable UUID sessionId,
            HttpServletRequest request) {

        log.info("statusCallback() — sessionId={}", sessionId);

        if (!normalizer.validateSignature(request)) {
            log.warn("statusCallback() — invalid signature rejected for session {}", sessionId);
            return ResponseEntity.status(403).build();
        }

        var event = normalizer.normalizeStatusCallback(request);
        if (event != null) {
            callService.updateStatus(sessionId, event.status(), event.durationSeconds());
            log.info("statusCallback() — session {} → {}", sessionId, event.status());
        }

        return ResponseEntity.noContent().build();
    }

    // ── Recording callback ────────────────────────────────────────────────────

    /**
     * Called by the provider when a call recording is available.
     * Saves the recording URL to the session for later playback.
     */
    @PostMapping("/recording/{sessionId}")
    public ResponseEntity<Void> recordingCallback(
            @PathVariable UUID sessionId,
            HttpServletRequest request) {

        log.info("recordingCallback() — sessionId={}", sessionId);

        if (!normalizer.validateSignature(request)) {
            log.warn("recordingCallback() — invalid signature rejected for session {}", sessionId);
            return ResponseEntity.status(403).build();
        }

        var recordingUrl = normalizer.extractRecordingUrl(request);
        if (recordingUrl != null && !recordingUrl.isBlank()) {
            callService.saveRecording(sessionId, recordingUrl);
            log.info("recordingCallback() — recording saved for session {}", sessionId);
        }

        return ResponseEntity.noContent().build();
    }

    // ── Browser WebRTC ────────────────────────────────────────────────────────

    /**
     * Voice webhook for the Twilio TwiML App (browser-based WebRTC calling).
     * Only active when {@code calling.provider=twilio}; returns 501 for other providers.
     *
     * <p>The browser passes {@code leadId} and {@code tenantId} as custom params
     * when connecting via the Twilio Client SDK.
     */
    @PostMapping("/browser")
    public ResponseEntity<String> browserVoice(
            @RequestParam(required = false) String leadId,
            @RequestParam(required = false) String tenantId,
            HttpServletRequest request) {

        log.info("browserVoice() — leadId={} tenantId={} provider={}",
                leadId, tenantId, callingProvider.providerName());

        if (!"twilio".equals(callingProvider.providerName())) {
            log.warn("browserVoice() — browser calling is only supported with Twilio (active: {})",
                    callingProvider.providerName());
            return ResponseEntity.status(501).build();
        }

        if (!normalizer.validateSignature(request)) {
            return ResponseEntity.status(403).build();
        }

        if (leadId == null || tenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        // Look up the most recent call session for this lead to get the real number
        var sessions = repo.findByTenantIdAndLeadId(
                UUID.fromString(tenantId),
                UUID.fromString(leadId),
                org.springframework.data.domain.PageRequest.of(
                        0, 1,
                        org.springframework.data.domain.Sort.by("startedAt").descending()));

        if (sessions.isEmpty()) {
            log.warn("browserVoice() — no session found for lead={}", leadId);
            return ResponseEntity.notFound().build();
        }

        var session  = sessions.getContent().get(0);
        // decryptForTwilio() is the ONLY caller here — do NOT log the result
        var realPhone = maskingService.decryptForTwilio(session);

        var response = normalizer.generateVoiceResponse(
                realPhone, callingProvider.fromNumber(), true);

        log.info("browserVoice() — response generated for session {}", session.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(response.contentType()))
                .body(response.body());
    }
}