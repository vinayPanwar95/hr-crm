package com.fms.hr_crm.lead.model.dto;

import java.util.UUID;

/**
 * Read-only view of a recruiter, returned to UI and API callers.
 * {@code leadsAssigned} is the count of active (non-closed) leads currently assigned.
 */
public record RecruiterResponse(
        UUID id,
        String name,
        String email,
        String phone,
        boolean active,
        long leadsAssigned
) {}