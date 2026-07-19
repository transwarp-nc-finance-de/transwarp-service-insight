CREATE TRIGGER precheck_retrieval_audit_immutable
BEFORE UPDATE, DELETE ON precheck_retrieval_audit
FOR EACH ROW CALL 'com.transwarp.serviceinsight.knowledge.publication.infrastructure.ImmutableIndexHistoryTrigger';

CREATE TRIGGER precheck_retrieval_candidate_immutable
BEFORE UPDATE, DELETE ON precheck_retrieval_candidate
FOR EACH ROW CALL 'com.transwarp.serviceinsight.knowledge.publication.infrastructure.ImmutableIndexHistoryTrigger';

CREATE TRIGGER precheck_evidence_immutable
BEFORE UPDATE, DELETE ON precheck_evidence
FOR EACH ROW CALL 'com.transwarp.serviceinsight.knowledge.publication.infrastructure.ImmutableIndexHistoryTrigger';
