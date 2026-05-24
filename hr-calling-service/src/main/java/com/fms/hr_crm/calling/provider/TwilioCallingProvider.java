package com.fms.hr_crm.calling.provider;

import com.fms.hr_crm.calling.config.TwilioProperties;
import com.fms.hr_crm.calling.model.entity.CallStatus;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.http.HttpMethod;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

/**
 * {@link CallingProvider} backed by the Twilio Programmable Voice API.
 *
 * <p>Instantiated by {@link com.fms.hr_crm.calling.config.CallingProviderConfig}
 * when {@code calling.provider=twilio} (the default).
 *
 * <p>Required environment variables:
 * <pre>
 *   TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER, TWILIO_WEBHOOK_BASE_URL
 * </pre>
 */
@Slf4j
public class TwilioCallingProvider implements CallingProvider {

    private final TwilioProperties props;

    public TwilioCallingProvider(TwilioProperties props) {
        this.props = props;
        initSdk();
    }

    private void initSdk() {
        var sid   = props.accountSid();
        var token = props.authToken();
        if (sid != null && !sid.isBlank() && token != null && !token.isBlank()
                && !sid.equals("ACtest")) {
            Twilio.init(sid, token);
            log.info("[Twilio] SDK initialised (account: {})", sid);
        } else {
            log.warn("[Twilio] Credentials not set or are test placeholders — outbound calls will not work");
        }
    }

    @Override
    public ProviderCallResult initiateCall(
            String to,
            String voiceWebhookUrl,
            String statusCallbackUrl,
            String recordingCallbackUrl) {

        var call = Call.creator(
                        new PhoneNumber(to),
                        new PhoneNumber(props.fromNumber()),
                        URI.create(voiceWebhookUrl))
                .setStatusCallback(statusCallbackUrl)
                .setStatusCallbackMethod(HttpMethod.POST)
                .setRecord(true)
                .setRecordingStatusCallback(recordingCallbackUrl)
                .create();

        log.info("[Twilio] Call initiated — callSid={} to={}", call.getSid(), to);
        return new ProviderCallResult(call.getSid(), CallStatus.RINGING);
    }

    @Override
    public void cancelCall(String providerCallId) {
        try {
            Call.updater(providerCallId)
                    .setStatus(Call.UpdateStatus.CANCELED)
                    .update();
            log.info("[Twilio] Call {} canceled", providerCallId);
        } catch (ApiException e) {
            // 400 means the call already ended — safe to ignore
            log.warn("[Twilio] Cancel returned an error for {} (call may have already ended): {}",
                    providerCallId, e.getMessage());
        }
    }

    @Override public String providerName()    { return "twilio"; }
    @Override public String webhookBaseUrl()  { return props.webhookBaseUrl(); }
    @Override public String fromNumber()      { return props.fromNumber(); }
}