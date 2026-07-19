CREATE TABLE index_task (
    task_id UUID PRIMARY KEY,
    resource_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    revision_id UUID NOT NULL REFERENCES knowledge_draft_revision(revision_id),
    status VARCHAR(32) NOT NULL,
    fts_status VARCHAR(32) NOT NULL,
    embedding_status VARCHAR(32) NOT NULL,
    attempt INTEGER NOT NULL,
    max_attempts INTEGER NOT NULL,
    error_code VARCHAR(100) NULL,
    error_message VARCHAR(1000) NULL,
    error_retryable BOOLEAN NULL,
    next_retry_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NULL,
    completed_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT index_task_attempt_check CHECK (attempt BETWEEN 0 AND 3),
    CONSTRAINT index_task_max_attempts_check CHECK (max_attempts = 3)
);

CREATE TABLE knowledge_publication_idempotency_lock (
    command_type VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    PRIMARY KEY (command_type, idempotency_key)
);

CREATE TABLE knowledge_publication_idempotency (
    command_type VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(71) NOT NULL,
    version_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    task_id UUID NULL REFERENCES index_task(task_id),
    audit_event_id UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (command_type, idempotency_key)
);

CREATE TABLE index_task_attempt (
    task_id UUID NOT NULL REFERENCES index_task(task_id),
    attempt INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    fts_status VARCHAR(32) NOT NULL,
    embedding_status VARCHAR(32) NOT NULL,
    error_code VARCHAR(100) NULL,
    error_retryable BOOLEAN NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NULL,
    PRIMARY KEY (task_id, attempt),
    CONSTRAINT index_task_attempt_history_check CHECK (attempt BETWEEN 1 AND 3)
);

CREATE INDEX index_task_resource_lookup ON index_task(resource_id, created_at);
CREATE INDEX index_task_recovery_lookup ON index_task(status, next_retry_at, created_at);
