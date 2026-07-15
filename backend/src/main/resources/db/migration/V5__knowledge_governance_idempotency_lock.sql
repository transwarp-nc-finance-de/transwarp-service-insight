CREATE TABLE knowledge_governance_idempotency_lock (
    command_type VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    PRIMARY KEY (command_type, idempotency_key)
);
