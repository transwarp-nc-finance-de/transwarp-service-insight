CREATE TABLE knowledge_document (
    document_id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    product_line_code VARCHAR(100) NOT NULL REFERENCES catalog_product_line(code),
    product_line_display_name VARCHAR(200) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    created_by VARCHAR(100) NOT NULL REFERENCES local_identity(user_code),
    current_published_version_id UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    mock_data BOOLEAN NOT NULL
);

CREATE TABLE knowledge_original_file (
    file_id UUID PRIMARY KEY,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    content_hash VARCHAR(71) NOT NULL,
    size_bytes BIGINT NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT knowledge_original_file_hash_check CHECK (content_hash LIKE 'sha256:%')
);

CREATE TABLE knowledge_version_v2 (
    version_id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES knowledge_document(document_id),
    revision_number INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(100) NOT NULL REFERENCES local_identity(user_code),
    submitted_by VARCHAR(100) NULL,
    approved_by VARCHAR(100) NULL,
    original_file_id UUID NOT NULL REFERENCES knowledge_original_file(file_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    mock_data BOOLEAN NOT NULL,
    UNIQUE (document_id, revision_number)
);

CREATE TABLE knowledge_draft_revision (
    revision_id UUID PRIMARY KEY,
    version_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    revision_number INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    product_line_code VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (version_id, revision_number)
);

CREATE TABLE parse_task (
    task_id UUID PRIMARY KEY,
    resource_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    status VARCHAR(32) NOT NULL,
    attempt INTEGER NOT NULL,
    max_attempts INTEGER NOT NULL,
    error_code VARCHAR(100) NULL,
    error_message VARCHAR(1000) NULL,
    error_retryable BOOLEAN NULL,
    next_retry_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NULL,
    completed_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT parse_task_attempt_check CHECK (attempt BETWEEN 0 AND 3),
    CONSTRAINT parse_task_max_attempts_check CHECK (max_attempts = 3)
);

CREATE TABLE knowledge_parse_result (
    version_id UUID PRIMARY KEY REFERENCES knowledge_version_v2(version_id),
    task_id UUID NOT NULL UNIQUE REFERENCES parse_task(task_id),
    parser_version VARCHAR(64) NOT NULL,
    parse_result_hash VARCHAR(71) NOT NULL,
    parsed_block_count INTEGER NOT NULL,
    chunk_count INTEGER NOT NULL,
    chunking_rule_version VARCHAR(64) NOT NULL
);

CREATE TABLE knowledge_parsed_block (
    block_id UUID PRIMARY KEY,
    version_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    sequence INTEGER NOT NULL,
    structure_path VARCHAR(1000) NOT NULL,
    text_content TEXT NOT NULL,
    content_hash VARCHAR(71) NOT NULL,
    UNIQUE (version_id, sequence)
);

CREATE TABLE knowledge_chunk (
    chunk_id UUID PRIMARY KEY,
    version_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    sequence INTEGER NOT NULL,
    structure_path VARCHAR(1000) NOT NULL,
    text_content TEXT NOT NULL,
    token_count INTEGER NOT NULL,
    content_hash VARCHAR(71) NOT NULL,
    chunking_rule_version VARCHAR(64) NOT NULL,
    UNIQUE (version_id, sequence),
    CONSTRAINT knowledge_chunk_token_check CHECK (token_count BETWEEN 1 AND 400)
);

CREATE TABLE knowledge_parse_warning (
    warning_id UUID PRIMARY KEY,
    version_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    warning_code VARCHAR(64) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    structure_path VARCHAR(1000) NULL,
    occurrence_count INTEGER NOT NULL,
    CONSTRAINT knowledge_parse_warning_occurrence_check CHECK (occurrence_count > 0)
);

CREATE TABLE knowledge_ingestion_idempotency (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_hash VARCHAR(71) NOT NULL,
    document_id UUID NOT NULL REFERENCES knowledge_document(document_id),
    version_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    task_id UUID NOT NULL REFERENCES parse_task(task_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX parse_task_resource_lookup ON parse_task(resource_id, created_at);
CREATE INDEX knowledge_version_visibility ON knowledge_version_v2(version_id, created_by);
CREATE INDEX knowledge_block_page ON knowledge_parsed_block(version_id, sequence);
CREATE INDEX knowledge_chunk_page ON knowledge_chunk(version_id, sequence);
