package com.fms.hr_crm.calling.model.dto;

import java.util.UUID;

/**
 * Payload sent by hr-lead-service when a new recruiter is onboarded.
 * The {@code tempPassword} is a one-time password — the recruiter must change it on first login.
 */
public record ProvisionRecruiterRequest(
        UUID   recruiterId,
        String username,
        String email,
        String fullName,
        String tempPassword,
        UUID   tenantId
) {}