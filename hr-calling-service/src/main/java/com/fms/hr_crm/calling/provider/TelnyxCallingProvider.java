package com.fms.hr_crm.calling.provider;

import com.fms.hr_crm.calling.config.TelnyxProperties;
import com.fms.hr_crm.calling.model.entity.CallStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * {@link CallingProvider} backed by the Telnyx Call Control API (REST, no external SDK required).
 *
 * <p>Instantiated by {@link com.fms.hr_crm.calling.config.CallingProviderConfig}
 * when {@code calling.provider=telnyx}.
 *
 * <p>Telnyx uses TeXML (TwiML-compatible XML) for voice webhook responses, so the
 * same XML format used for Twilio/Plivo works here too.
 *
 * <p>API reference: <a href="https://developers.telnyx.com/api/call-control">Telnyx Call Control</a>
 *
 * <p>Required environment variables:
 * <pre>
 *   TELNYX_API_KEY, TELNYX_CONNECTION_ID, TELNYX_FROM_NUMBER, TELNYX_WEBHOOK_BASE_URL
 * </pre>
 *
 * <p><b>Optional SDK:</b> if you prefer the official SDK, add to pom.xml:
 * <pre>{@code
 * <dependency>
 *   <groupId>com.telnyx.sdk</groupId>
 *   <artifactId>telnyx-java</artifactId>
 *   <version>2.1.3</version>
 * </dependency>
 * }</pre>
 */
@Slf4j
public class TelnyxCallingProvider implements CallingProvider {

    private static final String TELNYX_API = "https://api.telnyx.com/v2";

    private final TelnyxProperties props;
    private final RestClient restClient;

    public TelnyxCallingProvider(TelnyxProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(TELNYX_API)
                .defaultHeaders(h -> {
                    h.setBearerAuth(props.apiKey());
                    h.setContentType(MediaType.APPLICATION_JSON);
                })
                .build();
        log.info("[Telnyx] Provider initialised (connectionId: {})", props.connectionId());
    }

    @Override
    public ProviderCallResult initiateCall(
            String to,
            String voiceWebhookUrl,
            String statusCallbackUrl,
            String recordingCallbackUrl) {

        var body = Map.of(
                "connection_id",       props.connectionId(),
                "to",                  to,
                "from",                props.fromNumber(),
                "webhook_url",         voiceWebhookUrl,
                "webhook_url_method",  "POST",
                "record_audio",        true,
                "record_audio_url",    recordingCallbackUrl
        );

        @SuppressWarnings("unchecked")
        var response = restClient.post()
                .uri("/calls")
                .body(body)
                .retrieve()
                .body(Map.class);

        @SuppressWarnings("unchecked")
        var data = response != null ? (Map<String, Object>) response.get("data") : null;
        var callControlId = data != null ? (String) data.get("call_control_id") : null;

        log.info("[Telnyx] Call initiated — callControlId={} to={}", callControlId, to);
        return new ProviderCallResult(callControlId, CallStatus.INITIATED);
    }

    @Override
    public void cancelCall(String providerCallId) {
        try {
            restClient.post()
                    .uri("/calls/" + providerCallId + "/actions/hangup")
                    .body(Map.of())
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Telnyx] Call {} hung up", providerCallId);
        } catch (Exception e) {
            log.warn("[Telnyx] Hangup failed for {} (call may already be ended): {}",
                    providerCallId, e.getMessage());
        }
    }

    @Override public String providerName()   { return "telnyx"; }
    @Override public String webhookBaseUrl() { return props.webhookBaseUrl(); }
    @Override public String fromNumber()     { return props.fromNumber(); }
}