package com.fms.hr_crm.calling.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * An AI Calling Campaign — admin configures how many parallel AI agents to run,
 * which leads to target, and a time window. The orchestrator drives the calls.
 */
@Entity
@Table(
        name = "ai_campaigns",
        indexes = {
                @Index(name = "idx_campaign_tenant",  columnList = "tenant_id"),
                @Index(name = "idx_campaign_status",  columnList = "status")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class AiCampaign {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    private String description;

    /**
     * Number of AI agent slots — i.e. how many parallel calls run simultaneously.
     * Capped at 10 in the service layer.
     */
    @Column(name = "agent_count", nullable = false)
    private int agentCount = 1;

    /**
     * Which lead status to target. Stored as VARCHAR so we don't couple this entity
     * to the hr-lead-service enum. Valid values: NEW, CONTACTED, INTERESTED.
     */
    @Column(name = "target_lead_status", nullable = false)
    private String targetLeadStatus = "NEW";

    /** Campaign operates only between these hours (tenant local time). */
    @Column(name = "window_start")
    private LocalTime windowStart;

    @Column(name = "window_end")
    private LocalTime windowEnd;

    /**
     * Date on which the campaign auto-starts at {@code windowStart}.
     * Null = must be started manually.
     */
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.DRAFT;

    // ── Runtime counters (updated atomically via @Modifying queries) ──────────

    @Column(name = "total_leads")
    private int totalLeads;

    @Column(name = "called_count")
    private int calledCount;

    @Column(name = "completed_count")
    private int completedCount;

    @Column(name = "failed_count")
    private int failedCount;

    // ── Timestamps ────────────────────────────────────────────────────────────

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "stopped_at")
    private Instant stoppedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}