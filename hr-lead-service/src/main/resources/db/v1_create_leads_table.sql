CREATE SCHEMA IF NOT EXISTS lead_svc;

CREATE TABLE lead_svc.leads (
                                id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                                tenant_id           UUID         NOT NULL,
                                name                VARCHAR(255) NOT NULL,
                                phone               VARCHAR(30)  NOT NULL,
                                email               VARCHAR(255),
                                company             VARCHAR(255),
                                position_required   VARCHAR(255),
                                status              VARCHAR(30)  NOT NULL DEFAULT 'NEW',
                                source              VARCHAR(50),
                                recruiter_id        UUID,
                                ai_score            SMALLINT,
                                ai_label            VARCHAR(10),
                                last_contacted_at   TIMESTAMPTZ,
                                created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                deleted_at          TIMESTAMPTZ
);

-- Duplicate detection: unique phone per tenant (excluding deleted)
CREATE UNIQUE INDEX idx_leads_unique_phone
    ON lead_svc.leads(tenant_id, phone)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX idx_leads_unique_email
    ON lead_svc.leads(tenant_id, email)
    WHERE deleted_at IS NULL AND email IS NOT NULL;

-- Performance indexes
CREATE INDEX idx_leads_tenant     ON lead_svc.leads(tenant_id);
CREATE INDEX idx_leads_recruiter  ON lead_svc.leads(tenant_id, recruiter_id);
CREATE INDEX idx_leads_status     ON lead_svc.leads(tenant_id, status);
CREATE INDEX idx_leads_created    ON lead_svc.leads(tenant_id, created_at DESC);