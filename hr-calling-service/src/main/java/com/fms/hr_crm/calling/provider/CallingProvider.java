package com.fms.hr_crm.calling.provider;

/**
 * Provider-agnostic abstraction over a telephony API (Twilio, Plivo, Telnyx, …).
 *
 * <p>The active implementation is selected at startup via {@code calling.provider}
 * in {@code application.yaml}. All other application code — {@link com.fms.hr_crm.calling.service.CallService},
 * controllers, etc. — must depend ONLY on this interface, never on a concrete provider class.
 *
 * <p>Switching providers requires one property change and a restart; zero code changes.
 */
public interface CallingProvider {

    /**
     * Initiates an outbound call to the given phone number.
     *
     * <p>The recruiter's phone is dialled first; when they answer, the provider
     * fetches the TwiML/PHML/TeXML from {@code voiceWebhookUrl} to bridge the
     * call to the lead.
     *
     * @param to                   E.164 phone number to dial (recruiter or lead, depending on flow)
     * @param voiceWebhookUrl      URL the provider calls to get bridging instructions
     * @param statusCallbackUrl    URL the provider POSTs status updates to
     * @param recordingCallbackUrl URL the provider POSTs when a recording is available
     * @return result containing the provider call ID and the initial call status
     */
    ProviderCallResult initiateCall(
            String to,
            String voiceWebhookUrl,
            String statusCallbackUrl,
            String recordingCallbackUrl);

    /**
     * Cancels or hangs up an active call.
     * Implementations must swallow provider exceptions and log instead of propagating,
     * because cancellation is always best-effort (the call may have already ended).
     *
     * @param providerCallId the call ID returned by {@link #initiateCall}
     */
    void cancelCall(String providerCallId);

    /**
     * Human-readable provider name used in logs (e.g. {@code "twilio"}).
     */
    String providerName();

    /**
     * Base URL that the provider will use to reach this service's webhook endpoints.
     * Must be publicly reachable (ngrok URL in local dev, real domain in production).
     */
    String webhookBaseUrl();

    /**
     * The virtual "from" number used for outbound calls (e.g. the Twilio DID).
     * Surfaced here so the webhook controller can use it as the callerId in TwiML responses.
     */
    String fromNumber();
}