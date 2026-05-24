package com.fms.hr_crm.lead.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(
        name = "leads",
        indexes = {
                @Index(name = "idx_leads_tenant",    columnList = "tenant_id"),
                @Index(name = "idx_leads_phone",     columnList = "phone"),
                @Index(name = "idx_leads_email",     columnList = "email"),
                @Index(name = "idx_leads_recruiter", columnList = "recruiter_id"),
                @Index(name = "idx_leads_status",    columnList = "status")
        }
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lead extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private String email;
    private String company;

    @Column(name = "position_required")
    private String positionRequired;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status;

    @Enumerated(EnumType.STRING)
    private LeadSource source;

    @Column(name = "recruiter_id")
    private UUID recruiterId;

    @Column(name = "ai_score")
    private Integer aiScore;                    // set by hr-ai-service

    @Column(name = "ai_label")
    @Enumerated(EnumType.STRING)
    private AiLabel aiLabel;                    // HOT / WARM / COLD

    @Column(name = "last_contacted_at")
    private java.time.Instant lastContactedAt;

    // Status transition — business rule enforced in the entity
    public void transitionTo(LeadStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition lead from " + status + " to " + newStatus);
        }
        this.status = newStatus;
    }
}
