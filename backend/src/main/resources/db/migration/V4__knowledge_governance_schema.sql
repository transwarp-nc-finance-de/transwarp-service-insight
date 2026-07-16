ALTER TABLE knowledge_draft_revision ADD COLUMN cleaned_text TEXT NULL;
ALTER TABLE knowledge_draft_revision ADD COLUMN cleaned_text_hash VARCHAR(71) NULL;
ALTER TABLE knowledge_draft_revision ADD COLUMN product_line_display_name VARCHAR(200) NULL;
ALTER TABLE knowledge_draft_revision ADD COLUMN parse_warning_notes TEXT NOT NULL DEFAULT '[]';
ALTER TABLE knowledge_draft_revision ADD COLUMN created_by VARCHAR(100) NULL;
ALTER TABLE knowledge_draft_revision ADD COLUMN mock_data BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE knowledge_draft_revision r
SET cleaned_text_hash = (
        SELECT f.content_hash
        FROM knowledge_version_v2 v
        JOIN knowledge_original_file f ON f.file_id = v.original_file_id
        WHERE v.version_id = r.version_id
    ),
    created_by = (
        SELECT v.created_by FROM knowledge_version_v2 v WHERE v.version_id = r.version_id
    ),
    product_line_display_name = (
        SELECT d.product_line_display_name
        FROM knowledge_version_v2 v
        JOIN knowledge_document d ON d.document_id = v.document_id
        WHERE v.version_id = r.version_id
    );
ALTER TABLE knowledge_draft_revision ALTER COLUMN cleaned_text_hash SET NOT NULL;
ALTER TABLE knowledge_draft_revision ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE knowledge_draft_revision ALTER COLUMN product_line_display_name SET NOT NULL;
ALTER TABLE knowledge_draft_revision ADD CONSTRAINT knowledge_draft_revision_hash_check CHECK (cleaned_text_hash LIKE 'sha256:%');
ALTER TABLE knowledge_draft_revision ADD CONSTRAINT knowledge_draft_revision_creator_fk FOREIGN KEY (created_by) REFERENCES local_identity(user_code);

ALTER TABLE knowledge_version_v2 ADD COLUMN current_draft_revision_id UUID NULL;
UPDATE knowledge_version_v2 v
SET current_draft_revision_id = (
    SELECT r.revision_id
    FROM knowledge_draft_revision r
    WHERE r.version_id = v.version_id
    ORDER BY r.revision_number DESC
    LIMIT 1
);
ALTER TABLE knowledge_version_v2 ADD CONSTRAINT knowledge_version_current_revision_fk FOREIGN KEY (current_draft_revision_id) REFERENCES knowledge_draft_revision(revision_id);

ALTER TABLE parse_task ADD COLUMN draft_revision_id UUID NULL;
UPDATE parse_task t
SET draft_revision_id = (
    SELECT v.current_draft_revision_id FROM knowledge_version_v2 v WHERE v.version_id = t.resource_id
);
ALTER TABLE parse_task ALTER COLUMN draft_revision_id SET NOT NULL;
ALTER TABLE parse_task ADD CONSTRAINT parse_task_draft_revision_fk FOREIGN KEY (draft_revision_id) REFERENCES knowledge_draft_revision(revision_id);

CREATE TABLE knowledge_revision_parse_result (
    task_id UUID PRIMARY KEY REFERENCES parse_task(task_id),
    version_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    revision_id UUID NOT NULL REFERENCES knowledge_draft_revision(revision_id),
    parser_version VARCHAR(64) NOT NULL,
    parse_result_hash VARCHAR(71) NOT NULL,
    parsed_block_count INTEGER NOT NULL,
    chunk_count INTEGER NOT NULL,
    chunking_rule_version VARCHAR(64) NOT NULL,
    UNIQUE (revision_id),
    CONSTRAINT knowledge_revision_parse_hash_check CHECK (parse_result_hash LIKE 'sha256:%')
);

CREATE TABLE knowledge_revision_parsed_block (
    block_id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES parse_task(task_id),
    revision_id UUID NOT NULL REFERENCES knowledge_draft_revision(revision_id),
    sequence INTEGER NOT NULL,
    structure_path VARCHAR(1000) NOT NULL,
    text_content TEXT NOT NULL,
    content_hash VARCHAR(71) NOT NULL,
    UNIQUE (revision_id, sequence)
);

CREATE TABLE knowledge_revision_chunk (
    chunk_id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES parse_task(task_id),
    revision_id UUID NOT NULL REFERENCES knowledge_draft_revision(revision_id),
    sequence INTEGER NOT NULL,
    structure_path VARCHAR(1000) NOT NULL,
    text_content TEXT NOT NULL,
    token_count INTEGER NOT NULL,
    content_hash VARCHAR(71) NOT NULL,
    chunking_rule_version VARCHAR(64) NOT NULL,
    UNIQUE (revision_id, sequence),
    CONSTRAINT knowledge_revision_chunk_token_check CHECK (token_count BETWEEN 1 AND 400)
);

CREATE TABLE knowledge_revision_parse_warning (
    warning_id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES parse_task(task_id),
    revision_id UUID NOT NULL REFERENCES knowledge_draft_revision(revision_id),
    warning_code VARCHAR(64) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    structure_path VARCHAR(1000) NULL,
    occurrence_count INTEGER NOT NULL,
    CONSTRAINT knowledge_revision_warning_occurrence_check CHECK (occurrence_count > 0)
);

INSERT INTO knowledge_revision_parse_result(task_id, version_id, revision_id, parser_version, parse_result_hash, parsed_block_count, chunk_count, chunking_rule_version)
SELECT r.task_id, r.version_id, t.draft_revision_id, r.parser_version, r.parse_result_hash, r.parsed_block_count, r.chunk_count, r.chunking_rule_version
FROM knowledge_parse_result r JOIN parse_task t ON t.task_id = r.task_id;
INSERT INTO knowledge_revision_parsed_block(block_id, task_id, revision_id, sequence, structure_path, text_content, content_hash)
SELECT b.block_id, t.task_id, t.draft_revision_id, b.sequence, b.structure_path, b.text_content, b.content_hash
FROM knowledge_parsed_block b JOIN parse_task t ON t.resource_id = b.version_id JOIN knowledge_parse_result r ON r.task_id = t.task_id;
INSERT INTO knowledge_revision_chunk(chunk_id, task_id, revision_id, sequence, structure_path, text_content, token_count, content_hash, chunking_rule_version)
SELECT c.chunk_id, t.task_id, t.draft_revision_id, c.sequence, c.structure_path, c.text_content, c.token_count, c.content_hash, c.chunking_rule_version
FROM knowledge_chunk c JOIN parse_task t ON t.resource_id = c.version_id JOIN knowledge_parse_result r ON r.task_id = t.task_id;
INSERT INTO knowledge_revision_parse_warning(warning_id, task_id, revision_id, warning_code, message, structure_path, occurrence_count)
SELECT w.warning_id, t.task_id, t.draft_revision_id, w.warning_code, w.message, w.structure_path, w.occurrence_count
FROM knowledge_parse_warning w JOIN parse_task t ON t.resource_id = w.version_id JOIN knowledge_parse_result r ON r.task_id = t.task_id;

CREATE TABLE knowledge_review_history (
    review_record_id UUID PRIMARY KEY,
    version_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    revision_id UUID NOT NULL REFERENCES knowledge_draft_revision(revision_id),
    action VARCHAR(32) NOT NULL,
    actor_user_code VARCHAR(100) NOT NULL REFERENCES local_identity(user_code),
    parse_result_hash VARCHAR(71) NULL,
    acknowledged_warning_codes VARCHAR(500) NOT NULL,
    reason VARCHAR(1000) NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE knowledge_governance_audit (
    event_id UUID PRIMARY KEY,
    actor_user_code VARCHAR(100) NOT NULL REFERENCES local_identity(user_code),
    action VARCHAR(64) NOT NULL,
    subject_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    outcome VARCHAR(32) NOT NULL,
    parse_result_hash VARCHAR(71) NULL,
    acknowledged_warning_codes VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    mock_data BOOLEAN NOT NULL
);

CREATE TABLE knowledge_governance_idempotency (
    command_type VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(71) NOT NULL,
    version_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    revision_id UUID NULL REFERENCES knowledge_draft_revision(revision_id),
    task_id UUID NULL REFERENCES parse_task(task_id),
    audit_event_id UUID NULL REFERENCES knowledge_governance_audit(event_id),
    result_status VARCHAR(32) NULL,
    result_submitted_by VARCHAR(100) NULL,
    result_approved_by VARCHAR(100) NULL,
    result_updated_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (command_type, idempotency_key)
);

CREATE INDEX knowledge_current_revision_lookup ON knowledge_version_v2(version_id, current_draft_revision_id);
CREATE INDEX knowledge_revision_task_lookup ON parse_task(draft_revision_id, created_at);
CREATE INDEX knowledge_review_history_lookup ON knowledge_review_history(version_id, occurred_at);
CREATE INDEX knowledge_governance_audit_lookup ON knowledge_governance_audit(subject_id, occurred_at);
