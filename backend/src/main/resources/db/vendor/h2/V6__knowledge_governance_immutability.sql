CREATE TRIGGER knowledge_draft_revision_immutable
BEFORE UPDATE, DELETE ON knowledge_draft_revision
FOR EACH ROW CALL 'com.transwarp.serviceinsight.knowledge.governance.infrastructure.ImmutableGovernanceTrigger';

CREATE TRIGGER knowledge_review_history_immutable
BEFORE UPDATE, DELETE ON knowledge_review_history
FOR EACH ROW CALL 'com.transwarp.serviceinsight.knowledge.governance.infrastructure.ImmutableGovernanceTrigger';
