CREATE TABLE knowledge_chunk_fts_stage (
    task_id UUID NOT NULL REFERENCES index_task(task_id),
    chunk_id UUID NOT NULL REFERENCES knowledge_revision_chunk(chunk_id),
    text_content CHARACTER LARGE OBJECT NOT NULL,
    fts_document CHARACTER LARGE OBJECT NOT NULL,
    PRIMARY KEY (task_id, chunk_id)
);
