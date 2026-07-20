CREATE FUNCTION reject_interaction_history_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'interaction and audit history is immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER precheck_feedback_v2_immutable
BEFORE UPDATE OR DELETE ON precheck_feedback_v2
FOR EACH ROW EXECUTE FUNCTION reject_interaction_history_mutation();

CREATE TRIGGER submission_continuation_v2_immutable
BEFORE UPDATE OR DELETE ON submission_continuation_v2
FOR EACH ROW EXECUTE FUNCTION reject_interaction_history_mutation();

CREATE TRIGGER audit_event_v2_immutable
BEFORE UPDATE OR DELETE ON audit_event_v2
FOR EACH ROW EXECUTE FUNCTION reject_interaction_history_mutation();
