package com.fms.hr_crm.calling.model.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Payload for creating a new AI calling campaign.
 *
 * @param agentCount    number of parallel AI call slots (1–10)
 * @param scheduledDate optional — when set the campaign auto-starts at windowStart on that date
 */
public record CampaignRequest(
        String    name,
        String    description,
        int       agentCount,
        String    targetLeadStatus,
        LocalTime windowStart,
        LocalTime windowEnd,
        LocalDate scheduledDate
) {}