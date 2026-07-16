CREATE FUNCTION reject_precheck_run_mutation() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'precheck run snapshots are immutable';
END;
$$;

CREATE TRIGGER precheck_run_v2_immutable
BEFORE UPDATE OR DELETE ON precheck_run_v2
FOR EACH ROW EXECUTE FUNCTION reject_precheck_run_mutation();
