package com.fms.hr_crm.lead.client;

/**
 * Payload sent to hr-calling-service to provision a recruiter's login account.
 */
import java.util.UUID;

public record ProvisionRecruiterRequest(
        UUID   recruiterId,
        String username,
        String email,
        String fullName,
        String tempPassword,
        UUID   tenantId
) {}