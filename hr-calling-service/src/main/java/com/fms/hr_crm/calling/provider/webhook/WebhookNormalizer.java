package com.fms.hr_crm.calling.provider.webhook;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Provider-agnostic abstraction over inbound webhook handling.
 *
 * <p>Each provider sends webhooks in a different format:
 * <ul>
 *   <li><b>Twilio</b> — form-encoded POST, HMAC-SHA1 signature in {@code X-Twilio-Signature}</li>
 *   <li><b>Plivo</b>  — form-encoded POST, HMAC-SHA256 signature in {@code X-Plivo-Signature-V2}</li>
 *   <li><b>Telnyx</b> — JSON POST, Ed25519 signature in {@code Telnyx-Signature-Ed25519}</li>
 * </ul>
 *
 * <p>The active implementation is injected by {@link com.fms.hr_crm.calling.config.CallingProviderConfig}
 * based on {@code calling.provider}. The generic {@link com.fms.hr_crm.calling.controller.WebhookController}
 * depends only on this interface.
 */
public interface WebhookNormalizer {

    /**
     * Validates that the inbound webhook genuinely originated from the provider.
     * Returns {@code true} to allow processing; {@code false} to reject with 403.
     *
     * <p>Implementations must skip validation and return {@code true} when running
     * with placeholder / test credentials so that local dev works without a tunnel.
     */
    boolean validateSignature(HttpServletRequest request);

    /**
     * Parses a status-callback webhook into our internal {@link WebhookEvent}.
     * Called for every status update (ringing → in-progress → completed, etc.).
     *
     * @return parsed event, or {@code null} if the payload is not a status update
     */
    WebhookEvent normalizeStatusCallback(HttpServletRequest request);

    /**
     * Generates the voice-instruction response body that the provider expects when it
     * calls the voice webhook (i.e. "dial this number").
     *
     * @param targetNumber E.164 number to connect the caller to
     * @param callerId     the virtual "from" number shown on the callee's screen
     * @param record       whether to record the call
     */
    WebhookResponse generateVoiceResponse(String targetNumber, String callerId, boolean record);

    /**
     * Extracts the recording URL from a recording-callback webhook.
     *
     * @return fully qualified recording URL, or {@code null} if not present in the payload
     */
    String extractRecordingUrl(HttpServletRequest request);
}