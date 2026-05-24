package com.fms.hr_crm.calling.provider;

import com.fms.hr_crm.calling.config.PlivoProperties;
import com.fms.hr_crm.calling.model.entity.CallStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * {@link CallingProvider} backed by the Plivo Voice API (REST, no external SDK required).
 *
 * <p>Instantiated by {@link com.fms.hr_crm.calling.config.CallingProviderConfig}
 * when {@code calling.provider=plivo}.
 *
 * <p>API reference: <a href="https://www.plivo.com/docs/voice/api/call/">Plivo Call API</a>
 *
 * <p>Required environment variables:
 * <pre>
 *   PLIVO_AUTH_ID, PLIVO_AUTH_TOKEN, PLIVO_FROM_NUMBER, PLIVO_WEBHOOK_BASE_URL
 * </pre>
 *
 * <p><b>Optional SDK:</b> if you prefer the official SDK, add to pom.xml:
 * <pre>{@code
 * <dependency>
 *   <groupId>com.plivo</groupId>
 *   <artifactId>plivo-java</artifactId>
 *   <version>5.7.0</version>
 * </dependency>
 * }</pre>
 * and replace the RestClient calls with {@code RestClient client = new RestClient(authId, authToken)}.
 */
@Slf4j
public class PlivoCallingProvider implements CallingProvider {

    private static final String PLIVO_API = "https://api.plivo.com/v1";

    private final PlivoProperties props;
    private final RestClient restClient;

    public PlivoCallingProvider(PlivoProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(PLIVO_API + "/Account/" + props.authId())
                .defaultHeaders(h -> {
                    h.setBasicAuth(props.authId(), props.authToken());
                    h.setContentType(MediaType.APPLICATION_JSON);
                })
                .build();
        log.info("[Plivo] Provider initialised (authId: {})", props.authId());
    }

    @Override
    public ProviderCallResult initiateCall(
            String to,
            String voiceWebhookUrl,
            String statusCallbackUrl,
            String recordingCallbackUrl) {

        // Plivo expects to/from as comma-separated strings for bulk dialling;
        // single number still works with the same format.
        var body = Map.of(
                "to",                     to,
                "from",                   props.fromNumber(),
                "answer_url",             voiceWebhookUrl,
                "answer_method",          "POST",
                "hangup_url",             statusCallbackUrl,
                "hangup_method",          "POST",
                "record",                 "true",
                "record_callback_url",    recordingCallbackUrl,
                "record_callback_method", "POST"
        );

        @SuppressWarnings("unchecked")
        var response = restClient.post()
                .uri("/Call/")
                .body(body)
                .retrieve()
                .body(Map.class);

        var requestUuid = response != null ? (String) response.get("request_uuid") : null;
        log.info("[Plivo] Call initiated — requestUuid={} to={}", requestUuid, to);
        return new ProviderCallResult(requestUuid, CallStatus.INITIATED);
    }

    @Override
    public void cancelCall(String providerCallId) {
        try {
            restClient.delete()
                    .uri("/Call/" + providerCallId + "/")
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Plivo] Call {} terminated", providerCallId);
        } catch (Exception e) {
            log.warn("[Plivo] Cancel failed for {} (call may already be ended): {}",
                    providerCallId, e.getMessage());
        }
    }

    @Override public String providerName()   { return "plivo"; }
    @Override public String webhookBaseUrl() { return props.webhookBaseUrl(); }
    @Override public String fromNumber()     { return props.fromNumber(); }
}