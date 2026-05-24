package com.fms.hr_crm.lead.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * Represents a recruiter who can be assigned to leads.
 * Recruiters are onboarded by admin through the Recruiter Dashboard.
 */
@Entity
@Table(
        name = "recruiters",
        indexes = {
                @Index(name = "idx_recruiters_tenant",      columnList = "tenant_id"),
                @Index(name = "idx_recruiters_tenant_name", columnList = "tenant_id, name")
        }
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recruiter extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String email;

    private String phone;

    /** When false, the recruiter is deactivated and won't receive new leads. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}