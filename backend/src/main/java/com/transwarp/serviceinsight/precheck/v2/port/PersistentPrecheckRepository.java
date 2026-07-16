package com.transwarp.serviceinsight.precheck.v2.port;

import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.CompletenessPolicy;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckRun;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckSession;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.SessionTermination;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersistentPrecheckRepository {
  void lockBusinessKey(String sourceSystem, String hostRequestId);

  Optional<BusinessKeyRecord> findBusinessKey(String sourceSystem, String hostRequestId);

  CompletenessPolicy findPolicy(String issueTypeCode);

  void create(PrecheckSession session, PrecheckRun run, String contextHash);

  Optional<PrecheckSession> findSession(UUID sessionId);

  void lockCommand(String commandType, String idempotencyKey);

  Optional<RunIdempotencyRecord> findRunIdempotency(String idempotencyKey);

  Optional<PrecheckSession> lockSession(UUID sessionId);

  void appendRun(PrecheckRun run, String idempotencyKey, String requestHash);

  Optional<TerminationIdempotencyRecord> findTerminationIdempotency(String idempotencyKey);

  SessionTermination terminate(
      UUID sessionId, String idempotencyKey, String requestHash, java.time.Instant terminatedAt);

  List<PrecheckSession> findSessionsByOwner(String ownerUserCode);

  List<PrecheckRun> findRuns(UUID sessionId);

  Optional<PrecheckRun> findRun(UUID sessionId, UUID runId);

  List<CompletenessPolicy> findPolicies(String issueTypeCode);

  Optional<CompletenessPolicy> findPolicyVersion(String policyVersion);

  record BusinessKeyRecord(UUID sessionId, String contextHash) {}

  record RunIdempotencyRecord(String requestHash, PrecheckRun run) {}

  record TerminationIdempotencyRecord(String requestHash, SessionTermination termination) {}
}
