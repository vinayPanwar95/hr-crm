package com.fms.hr_crm.calling.model.dto;

/**
 * Twilio Access Token for browser-based (WebRTC) calling via Twilio Client SDK.
 */
public record TwilioTokenResponse(String token, String identity) {}