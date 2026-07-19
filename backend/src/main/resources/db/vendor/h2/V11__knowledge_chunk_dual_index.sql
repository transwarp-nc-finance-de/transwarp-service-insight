CREATE TABLE knowledge_chunk_index (
    chunk_id UUID PRIMARY KEY REFERENCES knowledge_revision_chunk(chunk_id),
    version_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    revision_id UUID NOT NULL REFERENCES knowledge_draft_revision(revision_id),
    product_line_code VARCHAR(100) NOT NULL REFERENCES catalog_product_line(code),
    text_content CHARACTER LARGE OBJECT NOT NULL,
    fts_document CHARACTER LARGE OBJECT NOT NULL,
    embedding CHARACTER LARGE OBJECT NOT NULL,
    embedding_dimensions INTEGER NOT NULL,
    embedding_model VARCHAR(200) NOT NULL,
    embedding_revision VARCHAR(40) NOT NULL,
    content_hash VARCHAR(71) NOT NULL,
    indexed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT knowledge_chunk_embedding_dimensions_check CHECK (embedding_dimensions = 768)
);

CREATE INDEX knowledge_chunk_visibility_index ON knowledge_chunk_index(version_id, product_line_code, chunk_id);

CREATE TRIGGER knowledge_chunk_index_immutable
BEFORE UPDATE, DELETE ON knowledge_chunk_index
FOR EACH ROW CALL 'com.transwarp.serviceinsight.knowledge.publication.infrastructure.ImmutableIndexHistoryTrigger';

CREATE TRIGGER index_task_attempt_immutable
BEFORE UPDATE, DELETE ON index_task_attempt
FOR EACH ROW CALL 'com.transwarp.serviceinsight.knowledge.publication.infrastructure.ImmutableIndexHistoryTrigger';
