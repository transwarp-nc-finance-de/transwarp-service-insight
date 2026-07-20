CREATE TRIGGER precheck_feedback_v2_immutable
BEFORE UPDATE, DELETE ON precheck_feedback_v2
FOR EACH ROW CALL 'com.transwarp.serviceinsight.knowledge.publication.infrastructure.ImmutableIndexHistoryTrigger';

CREATE TRIGGER submission_continuation_v2_immutable
BEFORE UPDATE, DELETE ON submission_continuation_v2
FOR EACH ROW CALL 'com.transwarp.serviceinsight.knowledge.publication.infrastructure.ImmutableIndexHistoryTrigger';

CREATE TRIGGER audit_event_v2_immutable
BEFORE UPDATE, DELETE ON audit_event_v2
FOR EACH ROW CALL 'com.transwarp.serviceinsight.knowledge.publication.infrastructure.ImmutableIndexHistoryTrigger';
