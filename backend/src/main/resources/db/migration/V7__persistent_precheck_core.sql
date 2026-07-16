CREATE TABLE completeness_policy (
    policy_version VARCHAR(64) NOT NULL,
    issue_type_code VARCHAR(64) NOT NULL,
    issue_type_display_name VARCHAR(200) NOT NULL,
    general_field_codes VARCHAR(1000) NOT NULL,
    issue_specific_field_codes VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    mock_data BOOLEAN NOT NULL,
    PRIMARY KEY (policy_version, issue_type_code)
);

INSERT INTO completeness_policy(policy_version, issue_type_code, issue_type_display_name, general_field_codes, issue_specific_field_codes, created_at, mock_data) VALUES
    ('mock-completeness-v1', 'FUNCTIONAL_FAILURE', '功能故障（模拟数据）', 'PRODUCT,COMPONENT,VERSION,SEVERITY,IMPACT_SCOPE,OCCURRED_AT', 'ERROR_MESSAGE,REPRODUCTION_STEPS,RECENT_CHANGES', CURRENT_TIMESTAMP, TRUE),
    ('mock-completeness-v1', 'PERFORMANCE_DEGRADATION', '性能下降（模拟数据）', 'PRODUCT,COMPONENT,VERSION,SEVERITY,IMPACT_SCOPE,OCCURRED_AT', 'TIME_WINDOW,BASELINE_METRIC,CURRENT_METRIC,WORKLOAD_IDENTIFIER', CURRENT_TIMESTAMP, TRUE),
    ('mock-completeness-v1', 'INSTALLATION_CONFIGURATION', '安装配置（模拟数据）', 'PRODUCT,COMPONENT,VERSION,SEVERITY,IMPACT_SCOPE,OCCURRED_AT', 'OPERATION_GOAL,EXECUTED_STEPS,ENVIRONMENT_SUMMARY,ERROR_MESSAGE', CURRENT_TIMESTAMP, TRUE),
    ('mock-completeness-v1', 'DATA_CORRECTNESS', '数据正确性（模拟数据）', 'PRODUCT,COMPONENT,VERSION,SEVERITY,IMPACT_SCOPE,OCCURRED_AT', 'EXPECTED_RESULT,ACTUAL_RESULT,AFFECTED_DATA_SCOPE,QUERY_OR_JOB_ID', CURRENT_TIMESTAMP, TRUE);

CREATE TABLE precheck_business_key_lock (
    source_system VARCHAR(32) NOT NULL,
    host_request_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (source_system, host_request_id)
);

CREATE TABLE precheck_session_v2 (
    session_id UUID PRIMARY KEY,
    owner_user_code VARCHAR(100) NOT NULL REFERENCES local_identity(user_code),
    source_system VARCHAR(32) NOT NULL,
    host_request_id VARCHAR(100) NOT NULL,
    context_hash VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    termination_reason VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    mock_data BOOLEAN NOT NULL,
    UNIQUE (source_system, host_request_id),
    CONSTRAINT precheck_session_status_check CHECK (status IN ('ACTIVE', 'TERMINATED'))
);

CREATE TABLE precheck_run_v2 (
    run_id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES precheck_session_v2(session_id),
    sequence_number INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    context_snapshot TEXT NOT NULL,
    result_snapshot TEXT NOT NULL,
    policy_version VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (session_id, sequence_number),
    CONSTRAINT precheck_run_sequence_check CHECK (sequence_number BETWEEN 1 AND 3)
);

CREATE TABLE precheck_command_lock (
    command_type VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    PRIMARY KEY (command_type, idempotency_key)
);

CREATE TABLE precheck_command_idempotency (
    command_type VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    session_id UUID NOT NULL REFERENCES precheck_session_v2(session_id),
    run_id UUID REFERENCES precheck_run_v2(run_id),
    audit_event_id UUID,
    terminated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (command_type, idempotency_key)
);
