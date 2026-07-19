ALTER TABLE index_task ADD COLUMN actor_user_code VARCHAR(100) NULL;

UPDATE index_task t
SET actor_user_code = (
    SELECT v.approved_by FROM knowledge_version_v2 v WHERE v.version_id = t.resource_id
);

ALTER TABLE index_task ALTER COLUMN actor_user_code SET NOT NULL;
ALTER TABLE index_task ADD CONSTRAINT index_task_actor_fk
    FOREIGN KEY (actor_user_code) REFERENCES local_identity(user_code);
