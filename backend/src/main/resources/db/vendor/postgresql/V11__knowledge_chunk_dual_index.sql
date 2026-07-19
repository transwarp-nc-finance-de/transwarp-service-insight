CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge_chunk_index (
    chunk_id UUID PRIMARY KEY REFERENCES knowledge_revision_chunk(chunk_id),
    version_id UUID NOT NULL REFERENCES knowledge_version_v2(version_id),
    revision_id UUID NOT NULL REFERENCES knowledge_draft_revision(revision_id),
    product_line_code VARCHAR(100) NOT NULL REFERENCES catalog_product_line(code),
    text_content TEXT NOT NULL,
    fts_document TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', text_content)) STORED,
    embedding VECTOR(768) NOT NULL,
    embedding_model VARCHAR(200) NOT NULL,
    embedding_revision VARCHAR(40) NOT NULL,
    content_hash VARCHAR(71) NOT NULL,
    indexed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX knowledge_chunk_fts_index ON knowledge_chunk_index USING GIN (fts_document);
CREATE INDEX knowledge_chunk_vector_index ON knowledge_chunk_index USING hnsw (embedding vector_cosine_ops);
CREATE INDEX knowledge_chunk_visibility_index ON knowledge_chunk_index(version_id, product_line_code, chunk_id);

CREATE FUNCTION reject_dual_index_history_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'dual index history is immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER knowledge_chunk_index_immutable
BEFORE UPDATE OR DELETE ON knowledge_chunk_index
FOR EACH ROW EXECUTE FUNCTION reject_dual_index_history_mutation();

CREATE TRIGGER index_task_attempt_immutable
BEFORE UPDATE OR DELETE ON index_task_attempt
FOR EACH ROW EXECUTE FUNCTION reject_dual_index_history_mutation();
