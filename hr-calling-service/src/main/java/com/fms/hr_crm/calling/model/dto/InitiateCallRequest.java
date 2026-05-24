package com.fms.hr_crm.calling.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request payload to initiate a masked outbound call.
 *
 * <p>Note: no phone numbers are accepted or returned here.
 * The real phone number is fetched internally from hr-lead-service.
 */
public record InitiateCallRequest(
        @NotNull UUID recruiterId,
        @NotNull UUID leadId,
        @NotNull UUID tenantId,
        /** Recruiter's own callback phone — Twilio dials this first, then bridges to lead. */
        String recruiterPhone
) {}