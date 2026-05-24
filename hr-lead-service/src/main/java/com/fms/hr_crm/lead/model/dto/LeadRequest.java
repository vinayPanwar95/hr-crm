package com.fms.hr_crm.lead.model.dto;

import com.fms.hr_crm.lead.model.entity.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record LeadRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Phone is required") String phone,
        @Email(message = "Invalid email format") String email,
        String company,
        String positionRequired,
        LeadSource source,
        UUID recruiterId
) {}