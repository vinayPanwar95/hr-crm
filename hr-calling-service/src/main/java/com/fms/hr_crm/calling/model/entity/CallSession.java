package com.fms.hr_crm.calling.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single call session initiated by a recruiter toward a lead.
 *
 * <p>SECURITY RULE: {@code realNumberEncrypted} must ONLY be read or written
 * through {@link com.fms.hr_crm.calling.service.MaskingService}.
 * No other class may call {@link #getRealNumberEncrypted()} or
 * {@link #setRealNumberEncrypted(String)}.
 */
@Entity
@Table(
        name = "call_sessions",
        indexes = {
                @Index(name = "idx_cs_tenant",      columnList = "tenant_id"),
                @Index(name = "idx_cs_recruiter",   columnList = "recruiter_id"),
                @Index(name = "idx_cs_lead",        columnList = "lead_id"),
                @Index(name = "idx_cs_status",      columnList = "status"),
                @Index(name = "idx_cs_twilio_sid",  columnList = "twilio_call_sid")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class CallSession {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "recruiter_id", nullable = false)
    private UUID recruiterId;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    /** The Twilio number shown to the lead — not the lead's real number. */
    @Column(name = "virtual_from_number")
    private String virtualFromNumber;

    /**
     * AES-GCM encrypted real phone number of the lead.
     * ONLY {@link com.fms.hr_crm.calling.service.MaskingService} may access this field.
     */
    @Column(name = "real_number_encrypted", columnDefinition = "TEXT")
    private String realNumberEncrypted;

    @Column(name = "twilio_call_sid")
    private String twilioCallSid;

    /** Recruiter's callback phone (for outbound dial-to-recruiter flow). Stored as-is — not a lead number. */
    @Column(name = "recruiter_phone")
    private String recruiterPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatus status = CallStatus.INITIATED;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "recording_url", columnDefinition = "TEXT")
    private String recordingUrl;

    /**
     * Auto-tagged after call completion: HOT_LEAD, CALL_BACK, NOT_INTERESTED, etc.
     * Set by auto-tagging logic based on call duration and outcome.
     */
    @Column(name = "call_tag")
    private String callTag;

    @CreatedDate
    @Column(name = "started_at", updatable = false)
    private Instant startedAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    /**
     * Set when this call belongs to an AI campaign.
     * Null for manually initiated recruiter calls.
     * Used by {@link com.fms.hr_crm.calling.service.AiCampaignOrchestrator} to route
     * call-completed events back to the campaign.
     */
    @Column(name = "campaign_id")
    private UUID campaignId;
}