package com.fms.hr_crm.calling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Plivo configuration properties, bound when {@code calling.provider=plivo}.
 *
 * <p>Set via environment variables:
 * <pre>
 *   PLIVO_AUTH_ID          — your Plivo account Auth ID
 *   PLIVO_AUTH_TOKEN       — your Plivo account Auth Token
 *   PLIVO_FROM_NUMBER      — E.164 number purchased in the Plivo console
 *   PLIVO_WEBHOOK_BASE_URL — publicly reachable base URL (ngrok in local dev)
 * </pre>
 */
@ConfigurationProperties(prefix = "plivo")
public record PlivoProperties(
        String authId,
        String authToken,
        String fromNumber,
        String webhookBaseUrl
) {}