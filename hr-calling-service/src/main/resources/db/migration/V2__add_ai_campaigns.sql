-- AI Campaign table
CREATE TABLE ai_campaigns (
    id                  UUID         PRIMARY KEY,
    tenant_id           UUID         NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    agent_count         SMALLINT     NOT NULL DEFAULT 1,
    target_lead_status  VARCHAR(30)  NOT NULL DEFAULT 'NEW',
    window_start        TIME,
    window_end          TIME,
    scheduled_date      DATE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    total_leads         INTEGER      NOT NULL DEFAULT 0,
    called_count        INTEGER      NOT NULL DEFAULT 0,
    completed_count     INTEGER      NOT NULL DEFAULT 0,
    failed_count        INTEGER      NOT NULL DEFAULT 0,
    started_at          TIMESTAMPTZ,
    stopped_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_campaign_tenant ON ai_campaigns (tenant_id);
CREATE INDEX idx_campaign_status ON ai_campaigns (status);

-- Add campaign_id to call_sessions so calls can be linked to a campaign
ALTER TABLE call_sessions ADD COLUMN campaign_id UUID;
CREATE INDEX idx_cs_campaign ON call_sessions (campaign_id);