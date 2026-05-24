package com.fms.hr_crm.lead.model.dto;

import com.fms.hr_crm.lead.model.entity.LeadStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionRequest(
        @NotNull(message = "New status is required") LeadStatus newStatus,
        String note
) {}