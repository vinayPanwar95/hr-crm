CREATE TABLE call_sessions (
    id                   UUID         PRIMARY KEY,
    tenant_id            UUID         NOT NULL,
    recruiter_id         UUID         NOT NULL,
    lead_id              UUID         NOT NULL,
    twilio_call_sid      VARCHAR(64),
    virtual_from_number  VARCHAR(20),
    real_number_encrypted TEXT,
    recruiter_phone      VARCHAR(20),
    status               VARCHAR(20)  NOT NULL DEFAULT 'INITIATED',
    duration_seconds     INTEGER,
    recording_url        TEXT,
    call_tag             VARCHAR(50),
    started_at           TIMESTAMPTZ,
    updated_at           TIMESTAMPTZ,
    ended_at             TIMESTAMPTZ
);

CREATE INDEX idx_cs_tenant      ON call_sessions (tenant_id);
CREATE INDEX idx_cs_recruiter   ON call_sessions (recruiter_id);
CREATE INDEX idx_cs_lead        ON call_sessions (lead_id);
CREATE INDEX idx_cs_status      ON call_sessions (status);
CREATE INDEX idx_cs_twilio_sid  ON call_sessions (twilio_call_sid);