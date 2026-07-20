CREATE TABLE submission_continuation_v2 (
    continuation_id UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE REFERENCES precheck_session_v2(session_id),
    confirmed_by VARCHAR(100) NOT NULL REFERENCES local_identity(user_code),
    product_line_code VARCHAR(100) NOT NULL REFERENCES catalog_product_line(code),
    confirmed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason VARCHAR(2000),
    audit_event_id UUID NOT NULL UNIQUE,
    mock_data BOOLEAN NOT NULL
);

CREATE TABLE submission_continuation_idempotency (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    continuation_id UUID NOT NULL REFERENCES submission_continuation_v2(continuation_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX submission_continuation_owner_lookup
    ON submission_continuation_v2(confirmed_by, continuation_id);
