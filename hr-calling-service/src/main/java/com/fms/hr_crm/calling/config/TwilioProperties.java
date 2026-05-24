package com.fms.hr_crm.calling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Twilio configuration properties — all values come from environment variables.
 */
@ConfigurationProperties(prefix = "twilio")
public record TwilioProperties(
        String accountSid,
        String authToken,
        String fromNumber,
        String twimlAppSid,
        String apiKeySid,
        String apiKeySecret,
        String webhookBaseUrl
) {}