CREATE TRIGGER precheck_run_v2_immutable
BEFORE UPDATE, DELETE ON precheck_run_v2
FOR EACH ROW CALL 'com.transwarp.serviceinsight.precheck.v2.infrastructure.ImmutablePrecheckRunTrigger';
