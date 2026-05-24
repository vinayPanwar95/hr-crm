package com.fms.hr_crm.calling.provider.webhook;

import com.fms.hr_crm.calling.model.entity.CallStatus;

/**
 * Provider-agnostic representation of a call status update received via webhook.
 * Produced by {@link WebhookNormalizer#normalizeStatusCallback} from provider-specific
 * payloads (Twilio form params, Plivo form params, Telnyx JSON).
 *
 * @param providerCallId the call ID as reported by the provider in the webhook
 * @param status         our internal status mapped from the provider's status string
 * @param durationSeconds call duration if the call has ended; null if still active
 */
public record WebhookEvent(
        String providerCallId,
        CallStatus status,
        Integer durationSeconds
) {}