CREATE TABLE password_reset_tokens (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token             VARCHAR(64) NOT NULL UNIQUE,
    recruiter_user_id UUID        NOT NULL REFERENCES recruiter_users(id) ON DELETE CASCADE,
    expires_at        TIMESTAMPTZ NOT NULL,
    used              BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prt_token ON password_reset_tokens (token);