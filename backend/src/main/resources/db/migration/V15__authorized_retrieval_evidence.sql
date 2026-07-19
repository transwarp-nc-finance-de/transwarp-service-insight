CREATE TABLE precheck_retrieval_audit (
    run_id UUID PRIMARY KEY REFERENCES precheck_run_v2(run_id),
    owner_user_code VARCHAR(100) NOT NULL REFERENCES local_identity(user_code),
    rule_version VARCHAR(64) NOT NULL,
    retrieval_mode VARCHAR(32) NOT NULL,
    fts_status_code VARCHAR(100) NOT NULL,
    embedding_status_code VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT precheck_retrieval_mode_check CHECK (retrieval_mode IN ('HYBRID', 'FTS_ONLY', 'UNAVAILABLE'))
);

CREATE TABLE precheck_retrieval_candidate (
    run_id UUID NOT NULL REFERENCES precheck_retrieval_audit(run_id),
    branch VARCHAR(16) NOT NULL,
    chunk_id UUID NOT NULL REFERENCES knowledge_chunk_index(chunk_id),
    branch_rank INTEGER NOT NULL,
    rrf_score DOUBLE PRECISION NULL,
    selected_rank INTEGER NULL,
    PRIMARY KEY (run_id, branch, chunk_id),
    CONSTRAINT precheck_candidate_branch_check CHECK (branch IN ('FTS', 'VECTOR')),
    CONSTRAINT precheck_candidate_rank_check CHECK (branch_rank BETWEEN 1 AND 20),
    CONSTRAINT precheck_selected_rank_check CHECK (selected_rank IS NULL OR selected_rank BETWEEN 1 AND 5)
);

CREATE TABLE precheck_evidence (
    evidence_id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES precheck_retrieval_audit(run_id),
    document_id UUID NOT NULL REFERENCES knowledge_document(document_id),
    version_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    chunk_id UUID NOT NULL REFERENCES knowledge_chunk_index(chunk_id),
    product_line_code VARCHAR(100) NOT NULL REFERENCES catalog_product_line(code),
    title VARCHAR(200) NOT NULL,
    excerpt VARCHAR(2000) NOT NULL,
    content_hash VARCHAR(71) NOT NULL,
    retrieval_mode VARCHAR(32) NOT NULL,
    fts_rank INTEGER NULL,
    vector_rank INTEGER NULL,
    rrf_score DOUBLE PRECISION NOT NULL,
    selected_rank INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (run_id, selected_rank),
    CONSTRAINT precheck_evidence_hash_check CHECK (content_hash LIKE 'sha256:%'),
    CONSTRAINT precheck_evidence_mode_check CHECK (retrieval_mode IN ('HYBRID', 'FTS_ONLY')),
    CONSTRAINT precheck_evidence_selected_rank_check CHECK (selected_rank BETWEEN 1 AND 5)
);

CREATE INDEX precheck_evidence_authorization_lookup ON precheck_evidence(evidence_id, product_line_code);
