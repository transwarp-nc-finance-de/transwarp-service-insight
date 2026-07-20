CREATE TABLE precheck_feedback_v2 (
    feedback_id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES precheck_session_v2(session_id),
    run_id UUID NOT NULL REFERENCES precheck_run_v2(run_id),
    owner_user_code VARCHAR(100) NOT NULL REFERENCES local_identity(user_code),
    product_line_code VARCHAR(100) NOT NULL REFERENCES catalog_product_line(code),
    adoption_status VARCHAR(32) NOT NULL,
    helpfulness VARCHAR(32),
    reason VARCHAR(2000),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    mock_data BOOLEAN NOT NULL,
    CONSTRAINT precheck_feedback_adoption_check CHECK (adoption_status IN ('ADOPTED', 'PARTIALLY_ADOPTED', 'IGNORED')),
    CONSTRAINT precheck_feedback_helpfulness_check CHECK (helpfulness IS NULL OR helpfulness IN ('HELPFUL', 'NOT_HELPFUL'))
);

CREATE TABLE precheck_feedback_idempotency (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    feedback_id UUID NOT NULL REFERENCES precheck_feedback_v2(feedback_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX precheck_feedback_owner_lookup
    ON precheck_feedback_v2(owner_user_code, feedback_id);
