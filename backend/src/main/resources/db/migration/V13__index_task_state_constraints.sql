ALTER TABLE index_task ADD CONSTRAINT index_task_status_check
    CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED'));
ALTER TABLE index_task ADD CONSTRAINT index_task_fts_status_check
    CHECK (fts_status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED'));
ALTER TABLE index_task ADD CONSTRAINT index_task_embedding_status_check
    CHECK (embedding_status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED'));
ALTER TABLE index_task ADD CONSTRAINT index_task_terminal_time_check
    CHECK ((status IN ('SUCCEEDED', 'FAILED')) = (completed_at IS NOT NULL));
ALTER TABLE index_task_attempt ADD CONSTRAINT index_task_attempt_status_check
    CHECK (status IN ('SUCCEEDED', 'FAILED'));
