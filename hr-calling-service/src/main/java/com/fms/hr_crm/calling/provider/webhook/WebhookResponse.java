package com.fms.hr_crm.calling.provider.webhook;

/**
 * The HTTP response body and content-type that the webhook controller returns
 * to the provider after a voice webhook.
 *
 * <ul>
 *   <li>Twilio expects TwiML XML — {@code text/xml}</li>
 *   <li>Plivo expects PHML XML — {@code text/xml}</li>
 *   <li>Telnyx TeXML expects XML — {@code text/xml}</li>
 * </ul>
 *
 * @param body        the raw response body string
 * @param contentType MIME type (use {@link org.springframework.http.MediaType} constants)
 */
public record WebhookResponse(String body, String contentType) {}