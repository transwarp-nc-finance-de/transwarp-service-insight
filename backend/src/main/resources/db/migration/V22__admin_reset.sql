CREATE TABLE admin_reset (
    task_id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    environment_code VARCHAR(32) NOT NULL,
    confirmed_by VARCHAR(100) NOT NULL,
    audit_event_id UUID NOT NULL UNIQUE,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt INTEGER NOT NULL,
    max_attempts INTEGER NOT NULL,
    error_code VARCHAR(100),
    error_message VARCHAR(1000),
    error_retryable BOOLEAN,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    mock_data BOOLEAN NOT NULL,
    CONSTRAINT admin_reset_environment_check CHECK (environment_code = 'LOCAL'),
    CONSTRAINT admin_reset_status_check CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT admin_reset_attempt_check CHECK (attempt BETWEEN 0 AND 3 AND max_attempts = 3)
);

CREATE TABLE admin_reset_idempotency_lock (
    idempotency_key VARCHAR(128) PRIMARY KEY
);

CREATE TABLE admin_reset_idempotency (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    task_id UUID NOT NULL REFERENCES admin_reset(task_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX admin_reset_history_lookup ON admin_reset(created_at, task_id);
