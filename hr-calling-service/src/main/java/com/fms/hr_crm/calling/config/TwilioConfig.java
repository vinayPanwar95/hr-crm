package com.fms.hr_crm.calling.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the Twilio SDK once at startup.
 * If credentials are missing (e.g. local dev), startup still succeeds but calls will fail.
 */
@Configuration
@ConditionalOnProperty(name = "calling.provider", havingValue = "twilio", matchIfMissing = true)
@EnableConfigurationProperties(TwilioProperties.class)
@RequiredArgsConstructor
@Slf4j
public class TwilioConfig {

    private final TwilioProperties props;

    @PostConstruct
    public void init() {
        var sid   = props.accountSid();
        var token = props.authToken();

        if (sid != null && !sid.isBlank() && token != null && !token.isBlank()
                && !sid.equals("ACtest")) {
            Twilio.init(sid, token);
            log.info("Twilio SDK initialized (account: {})", sid);
        } else {
            log.warn("Twilio credentials not configured or test placeholders detected — " +
                     "outbound calls will not work. Set TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN.");
        }
    }
}