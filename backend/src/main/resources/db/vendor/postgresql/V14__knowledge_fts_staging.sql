CREATE TABLE knowledge_chunk_fts_stage (
    task_id UUID NOT NULL REFERENCES index_task(task_id),
    chunk_id UUID NOT NULL REFERENCES knowledge_revision_chunk(chunk_id),
    text_content TEXT NOT NULL,
    fts_document TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', text_content)) STORED,
    PRIMARY KEY (task_id, chunk_id)
);

CREATE INDEX knowledge_chunk_fts_stage_index ON knowledge_chunk_fts_stage USING GIN (fts_document);
