package com.fms.hr_crm.calling.config;

import com.fms.hr_crm.calling.provider.CallingProvider;
import com.fms.hr_crm.calling.provider.PlivoCallingProvider;
import com.fms.hr_crm.calling.provider.TelnyxCallingProvider;
import com.fms.hr_crm.calling.provider.TwilioCallingProvider;
import com.fms.hr_crm.calling.provider.webhook.PlivoWebhookNormalizer;
import com.fms.hr_crm.calling.provider.webhook.TelnyxWebhookNormalizer;
import com.fms.hr_crm.calling.provider.webhook.TwilioWebhookNormalizer;
import com.fms.hr_crm.calling.provider.webhook.WebhookNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Factory that selects the active {@link CallingProvider} and {@link WebhookNormalizer}
 * based on the {@code calling.provider} property.
 *
 * <p>Switching providers requires a single line change in {@code application.yaml}:
 * <pre>
 * calling:
 *   provider: twilio   # → plivo | telnyx
 * </pre>
 *
 * <p>Startup fails immediately with a clear error if {@code calling.provider} is set
 * to an unrecognised value — avoiding silent misconfiguration.
 */
@Configuration
@EnableConfigurationProperties({TwilioProperties.class, PlivoProperties.class, TelnyxProperties.class})
@Slf4j
public class CallingProviderConfig {

    private static final Set<String> VALID_PROVIDERS = Set.of("twilio", "plivo", "telnyx");

    /**
     * Creates the active {@link CallingProvider} bean.
     * The Java 21 switch expression makes the exhaustive match visible at compile time.
     */
    @Bean
    public CallingProvider callingProvider(
            @Value("${calling.provider:twilio}") String provider,
            TwilioProperties twilioProps,
            PlivoProperties plivoProps,
            TelnyxProperties telnyxProps) {

        var name = provider.toLowerCase().trim();
        validate(name);

        var impl = switch (name) {
            case "twilio" -> new TwilioCallingProvider(twilioProps);
            case "plivo"  -> new PlivoCallingProvider(plivoProps);
            case "telnyx" -> new TelnyxCallingProvider(telnyxProps);
            default       -> throw new IllegalStateException("Unreachable — validated above");
        };

        log.info("Active calling provider: {} (webhookBase={})", impl.providerName(), impl.webhookBaseUrl());
        return impl;
    }

    /**
     * Creates the active {@link WebhookNormalizer} bean, paired with the provider above.
     */
    @Bean
    public WebhookNormalizer webhookNormalizer(
            @Value("${calling.provider:twilio}") String provider,
            TwilioProperties twilioProps,
            PlivoProperties plivoProps,
            TelnyxProperties telnyxProps) {

        return switch (provider.toLowerCase().trim()) {
            case "twilio" -> new TwilioWebhookNormalizer(twilioProps);
            case "plivo"  -> new PlivoWebhookNormalizer(plivoProps);
            case "telnyx" -> new TelnyxWebhookNormalizer(telnyxProps);
            default       -> throw new IllegalStateException("Unreachable — validated in callingProvider()");
        };
    }

    /**
     * Fails fast at startup with a human-readable error if the provider name is unknown.
     */
    private static void validate(String name) {
        if (!VALID_PROVIDERS.contains(name)) {
            throw new IllegalStateException(
                    """
                    ╔══════════════════════════════════════════════════════╗
                    ║  Unknown calling.provider: '%s'
                    ║  Valid values: twilio | plivo | telnyx
                    ║  Set the property in application.yaml or via env var CALLING_PROVIDER
                    ╚══════════════════════════════════════════════════════╝
                    """.formatted(name));
        }
    }
}