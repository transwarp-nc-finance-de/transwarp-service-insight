CREATE TABLE audit_event_v2 (
    event_id UUID PRIMARY KEY,
    actor_user_code VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    subject_type VARCHAR(100) NOT NULL,
    subject_id UUID NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    product_line_code VARCHAR(100) REFERENCES catalog_product_line(code),
    error_code VARCHAR(100),
    metadata TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    mock_data BOOLEAN NOT NULL,
    CONSTRAINT audit_event_outcome_check CHECK (outcome IN ('SUCCEEDED', 'FAILED', 'DENIED'))
);

CREATE INDEX audit_event_scope_lookup
    ON audit_event_v2(product_line_code, occurred_at, event_id);

CREATE INDEX audit_event_subject_lookup
    ON audit_event_v2(subject_id, occurred_at, event_id);
