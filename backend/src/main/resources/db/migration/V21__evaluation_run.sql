CREATE TABLE evaluation_run (
    task_id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    evaluation_set_version VARCHAR(64) NOT NULL,
    note VARCHAR(1000),
    requested_by VARCHAR(100) NOT NULL REFERENCES local_identity(user_code),
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
    summary_sample_count INTEGER,
    summary_gate_passed BOOLEAN,
    summary_permission_leakage_rate DOUBLE PRECISION,
    summary_citation_error_rate DOUBLE PRECISION,
    summary_degradation_pass_rate DOUBLE PRECISION,
    summary_recall_at_5 DOUBLE PRECISION,
    mock_data BOOLEAN NOT NULL,
    CONSTRAINT evaluation_run_status_check CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT evaluation_run_attempt_check CHECK (attempt BETWEEN 0 AND 3 AND max_attempts = 3)
);

CREATE TABLE evaluation_run_idempotency_lock (
    idempotency_key VARCHAR(128) PRIMARY KEY
);

CREATE TABLE evaluation_run_idempotency (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    task_id UUID NOT NULL REFERENCES evaluation_run(task_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE evaluation_case_result (
    task_id UUID NOT NULL REFERENCES evaluation_run(task_id),
    case_id VARCHAR(100) NOT NULL,
    scenario_tags TEXT NOT NULL,
    failed_checks TEXT NOT NULL,
    failure_codes TEXT NOT NULL,
    expected_summary TEXT NOT NULL,
    actual_summary TEXT NOT NULL,
    passed BOOLEAN NOT NULL,
    trace_session_id UUID,
    trace_run_id UUID,
    mock_data BOOLEAN NOT NULL,
    PRIMARY KEY (task_id, case_id)
);

CREATE INDEX evaluation_run_history_lookup ON evaluation_run(created_at, task_id);
CREATE INDEX evaluation_case_failure_lookup ON evaluation_case_result(task_id, passed, case_id);

ALTER TABLE precheck_retrieval_audit ADD COLUMN retrieval_duration_ms BIGINT NOT NULL DEFAULT 0;
ALTER TABLE precheck_retrieval_audit ADD COLUMN embedding_duration_ms BIGINT NOT NULL DEFAULT 0;
