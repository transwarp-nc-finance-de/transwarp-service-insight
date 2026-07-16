CREATE TRIGGER completeness_policy_immutable
BEFORE UPDATE, DELETE ON completeness_policy
FOR EACH ROW CALL 'com.transwarp.serviceinsight.precheck.v2.infrastructure.ImmutableCompletenessPolicyTrigger';
