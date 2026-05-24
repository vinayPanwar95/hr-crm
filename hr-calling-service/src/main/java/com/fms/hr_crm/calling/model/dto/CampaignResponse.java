package com.fms.hr_crm.calling.model.dto;

import com.fms.hr_crm.calling.model.entity.CampaignStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Read-only view of a campaign, polled by the UI for live progress updates.
 */
public record CampaignResponse(
        UUID           id,
        String         name,
        String         description,
        int            agentCount,
        String         targetLeadStatus,
        LocalTime      windowStart,
        LocalTime      windowEnd,
        LocalDate      scheduledDate,
        CampaignStatus status,
        int            totalLeads,
        int            calledCount,
        int            completedCount,
        int            failedCount,
        Instant        startedAt,
        Instant        stoppedAt,
        Instant        createdAt
) {
    /** 0–100 progress percentage for UI progress bars. */
    public int progressPct() {
        if (totalLeads == 0) return 0;
        return Math.min(100, calledCount * 100 / totalLeads);
    }
}