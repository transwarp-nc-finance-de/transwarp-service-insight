CREATE FUNCTION reject_knowledge_governance_mutation() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'knowledge governance records are immutable';
END;
$$;

CREATE TRIGGER knowledge_draft_revision_immutable
BEFORE UPDATE OR DELETE ON knowledge_draft_revision
FOR EACH ROW EXECUTE FUNCTION reject_knowledge_governance_mutation();

CREATE TRIGGER knowledge_review_history_immutable
BEFORE UPDATE OR DELETE ON knowledge_review_history
FOR EACH ROW EXECUTE FUNCTION reject_knowledge_governance_mutation();
