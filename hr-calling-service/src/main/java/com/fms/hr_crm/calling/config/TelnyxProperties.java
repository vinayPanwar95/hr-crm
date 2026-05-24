package com.fms.hr_crm.calling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Telnyx configuration properties, bound when {@code calling.provider=telnyx}.
 *
 * <p>Set via environment variables:
 * <pre>
 *   TELNYX_API_KEY          — API key from the Telnyx portal (starts with "KEY")
 *   TELNYX_CONNECTION_ID    — TeXML application or Call-Control connection ID
 *   TELNYX_FROM_NUMBER      — E.164 number purchased in the Telnyx portal
 *   TELNYX_WEBHOOK_BASE_URL — publicly reachable base URL (ngrok in local dev)
 *   TELNYX_WEBHOOK_PUBLIC_KEY (optional) — Ed25519 public key for full signature validation
 * </pre>
 */
@ConfigurationProperties(prefix = "telnyx")
public record TelnyxProperties(
        String apiKey,
        String connectionId,
        String fromNumber,
        String webhookBaseUrl,
        String webhookPublicKey
) {}