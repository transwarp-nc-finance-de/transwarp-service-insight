CREATE FUNCTION reject_retrieval_history_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'retrieval history is immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER precheck_retrieval_audit_immutable
BEFORE UPDATE OR DELETE ON precheck_retrieval_audit
FOR EACH ROW EXECUTE FUNCTION reject_retrieval_history_mutation();

CREATE TRIGGER precheck_retrieval_candidate_immutable
BEFORE UPDATE OR DELETE ON precheck_retrieval_candidate
FOR EACH ROW EXECUTE FUNCTION reject_retrieval_history_mutation();

CREATE TRIGGER precheck_evidence_immutable
BEFORE UPDATE OR DELETE ON precheck_evidence
FOR EACH ROW EXECUTE FUNCTION reject_retrieval_history_mutation();
