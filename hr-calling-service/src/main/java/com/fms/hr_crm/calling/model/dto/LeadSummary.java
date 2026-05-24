package com.fms.hr_crm.calling.model.dto;

import java.util.UUID;

/**
 * Lightweight lead info fetched from hr-lead-service for display in the calling UI.
 * Does NOT contain phone numbers — those are fetched separately via MaskingService.
 */
public record LeadSummary(
        UUID   id,
        String name,
        String status,
        String company,
        String positionRequired,
        UUID   recruiterId
) {}