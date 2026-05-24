package com.fms.hr_crm.calling.provider;

import com.fms.hr_crm.calling.model.entity.CallStatus;

/**
 * Returned by {@link CallingProvider#initiateCall} to carry the provider-assigned
 * call identifier and the initial status the provider reports after the API call.
 *
 * <p>The {@code callId} is stored on {@code CallSession.twilioCallSid} (field name is
 * intentionally kept for backwards compatibility — it stores any provider's call ID).
 *
 * @param callId        provider-specific call identifier (Twilio CallSid, Plivo RequestUUID, Telnyx call_control_id)
 * @param initialStatus status immediately after the API call — typically RINGING for Twilio, INITIATED for others
 */
public record ProviderCallResult(String callId, CallStatus initialStatus) {}