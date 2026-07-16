CREATE FUNCTION reject_completeness_policy_mutation() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'completeness policy versions are immutable';
END;
$$;

CREATE TRIGGER completeness_policy_immutable
BEFORE UPDATE OR DELETE ON completeness_policy
FOR EACH ROW EXECUTE FUNCTION reject_completeness_policy_mutation();
