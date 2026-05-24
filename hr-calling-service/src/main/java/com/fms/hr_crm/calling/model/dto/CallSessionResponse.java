package com.fms.hr_crm.calling.model.dto;

import com.fms.hr_crm.calling.model.entity.CallStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Recruiter-facing response for a call session.
 *
 * <p>MUST NOT include any phone numbers — real or virtual.
 * This is a GDPR / legal requirement enforced by convention.
 */
public record CallSessionResponse(
        UUID id,
        UUID recruiterId,
        UUID leadId,
        UUID tenantId,
        CallStatus status,
        Integer durationSeconds,
        String callTag,
        String recordingUrl,
        Instant startedAt,
        Instant endedAt,
        Instant updatedAt,
        UUID campaignId          // null for manual recruiter calls
) {}