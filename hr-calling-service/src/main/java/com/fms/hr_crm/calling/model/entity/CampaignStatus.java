package com.fms.hr_crm.calling.model.entity;

/**
 * Lifecycle states of an AI calling campaign.
 */
public enum CampaignStatus {
    /** Configured but not yet started. */
    DRAFT,
    /** Scheduled to auto-start at windowStart on scheduledDate. */
    SCHEDULED,
    /** AI agents are actively making calls. */
    RUNNING,
    /** Manually paused — can be resumed. */
    PAUSED,
    /** All leads in the queue have been called. */
    COMPLETED,
    /** Manually stopped by admin before completion. */
    STOPPED
}