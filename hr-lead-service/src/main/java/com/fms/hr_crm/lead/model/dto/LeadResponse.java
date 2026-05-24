package com.fms.hr_crm.lead.model.dto;

import com.fms.hr_crm.lead.model.entity.AiLabel;
import com.fms.hr_crm.lead.model.entity.LeadSource;
import com.fms.hr_crm.lead.model.entity.LeadStatus;

import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        UUID tenantId,
        String name,
        String phone,
        String email,
        String company,
        String positionRequired,
        LeadStatus status,
        LeadSource source,
        UUID recruiterId,
        Integer aiScore,
        AiLabel aiLabel,
        Instant lastContactedAt,
        Instant createdAt,
        Instant updatedAt
) {}