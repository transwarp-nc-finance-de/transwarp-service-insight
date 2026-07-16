package com.transwarp.serviceinsight.precheck.v2.infrastructure.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.CompletenessPolicy;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.IssueTypeValue;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckContext;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckResult;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckRun;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckSession;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.SessionTermination;
import com.transwarp.serviceinsight.precheck.v2.port.PersistentPrecheckRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPersistentPrecheckRepository implements PersistentPrecheckRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;

  public JdbcPersistentPrecheckRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  @Override
  public void lockBusinessKey(String sourceSystem, String hostRequestId) {
    jdbc.update(
        "INSERT INTO precheck_business_key_lock(source_system, host_request_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
        sourceSystem,
        hostRequestId);
    jdbc.queryForObject(
        "SELECT source_system FROM precheck_business_key_lock WHERE source_system = ? AND host_request_id = ? FOR UPDATE",
        String.class,
        sourceSystem,
        hostRequestId);
  }

  @Override
  public Optional<BusinessKeyRecord> findBusinessKey(String sourceSystem, String hostRequestId) {
    return jdbc
        .query(
            "SELECT session_id, context_hash FROM precheck_session_v2 WHERE source_system = ? AND host_request_id = ?",
            (resultSet, rowNumber) ->
                new BusinessKeyRecord(
                    resultSet.getObject("session_id", UUID.class),
                    resultSet.getString("context_hash")),
            sourceSystem,
            hostRequestId)
        .stream()
        .findFirst();
  }

  @Override
  public CompletenessPolicy findPolicy(String issueTypeCode) {
    var policies =
        jdbc.query(
            "SELECT * FROM completeness_policy WHERE policy_version = 'mock-completeness-v1' AND issue_type_code = ?",
            (resultSet, rowNumber) ->
                new CompletenessPolicy(
                    resultSet.getString("policy_version"),
                    new IssueTypeValue(
                        resultSet.getString("issue_type_code"),
                        resultSet.getString("issue_type_display_name")),
                    codes(resultSet.getString("general_field_codes")),
                    codes(resultSet.getString("issue_specific_field_codes")),
                    resultSet.getTimestamp("created_at").toInstant(),
                    resultSet.getBoolean("mock_data")),
            issueTypeCode);
    if (policies.isEmpty()) throw new IllegalArgumentException("Unknown issue type");
    return policies.getFirst();
  }

  @Override
  public void create(PrecheckSession session, PrecheckRun run, String contextHash) {
    jdbc.update(
        "INSERT INTO precheck_session_v2(session_id, owner_user_code, source_system, host_request_id, context_hash, status, termination_reason, created_at, updated_at, mock_data) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, TRUE)",
        session.sessionId(),
        session.ownerUserCode(),
        run.contextSnapshot().sourceSystem(),
        run.contextSnapshot().hostRequestId(),
        contextHash,
        session.status(),
        Timestamp.from(session.createdAt()),
        Timestamp.from(session.updatedAt()));
    jdbc.update(
        "INSERT INTO precheck_run_v2(run_id, session_id, sequence_number, status, context_snapshot, result_snapshot, policy_version, created_at, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        run.runId(),
        run.sessionId(),
        run.sequence(),
        run.status(),
        json(run.contextSnapshot()),
        json(run.result()),
        run.result().completeness().policyVersion(),
        Timestamp.from(run.createdAt()),
        Timestamp.from(run.completedAt()));
  }

  @Override
  public Optional<PrecheckSession> findSession(UUID sessionId) {
    return jdbc
        .query(
            "SELECT * FROM precheck_session_v2 WHERE session_id = ?",
            (resultSet, rowNumber) -> session(resultSet),
            sessionId)
        .stream()
        .findFirst();
  }

  @Override
  public void lockCommand(String commandType, String idempotencyKey) {
    jdbc.update(
        "INSERT INTO precheck_command_lock(command_type, idempotency_key) VALUES (?, ?) ON CONFLICT DO NOTHING",
        commandType,
        idempotencyKey);
    jdbc.queryForObject(
        "SELECT command_type FROM precheck_command_lock WHERE command_type = ? AND idempotency_key = ? FOR UPDATE",
        String.class,
        commandType,
        idempotencyKey);
  }

  @Override
  public Optional<RunIdempotencyRecord> findRunIdempotency(String idempotencyKey) {
    return jdbc
        .query(
            "SELECT request_hash, run_id FROM precheck_command_idempotency WHERE command_type = 'RUN' AND idempotency_key = ?",
            (resultSet, rowNumber) ->
                new RunIdempotencyRecord(
                    resultSet.getString("request_hash"),
                    loadRun(resultSet.getObject("run_id", UUID.class))),
            idempotencyKey)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<PrecheckSession> lockSession(UUID sessionId) {
    var locked =
        jdbc.query(
            "SELECT session_id FROM precheck_session_v2 WHERE session_id = ? FOR UPDATE",
            (resultSet, rowNumber) -> resultSet.getObject("session_id", UUID.class),
            sessionId);
    return locked.isEmpty() ? Optional.empty() : findSession(sessionId);
  }

  @Override
  public void appendRun(PrecheckRun run, String idempotencyKey, String requestHash) {
    jdbc.update(
        "INSERT INTO precheck_run_v2(run_id, session_id, sequence_number, status, context_snapshot, result_snapshot, policy_version, created_at, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        run.runId(),
        run.sessionId(),
        run.sequence(),
        run.status(),
        json(run.contextSnapshot()),
        json(run.result()),
        run.result().completeness().policyVersion(),
        Timestamp.from(run.createdAt()),
        Timestamp.from(run.completedAt()));
    jdbc.update(
        "UPDATE precheck_session_v2 SET updated_at = ? WHERE session_id = ?",
        Timestamp.from(run.completedAt()),
        run.sessionId());
    jdbc.update(
        "INSERT INTO precheck_command_idempotency(command_type, idempotency_key, request_hash, session_id, run_id, created_at) VALUES ('RUN', ?, ?, ?, ?, ?)",
        idempotencyKey,
        requestHash,
        run.sessionId(),
        run.runId(),
        Timestamp.from(run.completedAt()));
  }

  @Override
  public Optional<TerminationIdempotencyRecord> findTerminationIdempotency(String idempotencyKey) {
    return jdbc
        .query(
            "SELECT request_hash, session_id, audit_event_id, terminated_at FROM precheck_command_idempotency WHERE command_type = 'SELF_SERVICE' AND idempotency_key = ?",
            (resultSet, rowNumber) ->
                new TerminationIdempotencyRecord(
                    resultSet.getString("request_hash"),
                    new SessionTermination(
                        resultSet.getObject("session_id", UUID.class),
                        "TERMINATED",
                        "SELF_SERVICE_CONFIRMED",
                        resultSet.getTimestamp("terminated_at").toInstant(),
                        resultSet.getObject("audit_event_id", UUID.class),
                        true)),
            idempotencyKey)
        .stream()
        .findFirst();
  }

  @Override
  public SessionTermination terminate(
      UUID sessionId, String idempotencyKey, String requestHash, java.time.Instant terminatedAt) {
    var auditEventId = UUID.randomUUID();
    jdbc.update(
        "UPDATE precheck_session_v2 SET status = 'TERMINATED', termination_reason = 'SELF_SERVICE_CONFIRMED', updated_at = ? WHERE session_id = ?",
        Timestamp.from(terminatedAt),
        sessionId);
    jdbc.update(
        "INSERT INTO precheck_command_idempotency(command_type, idempotency_key, request_hash, session_id, audit_event_id, terminated_at, created_at) VALUES ('SELF_SERVICE', ?, ?, ?, ?, ?, ?)",
        idempotencyKey,
        requestHash,
        sessionId,
        auditEventId,
        Timestamp.from(terminatedAt),
        Timestamp.from(terminatedAt));
    return new SessionTermination(
        sessionId, "TERMINATED", "SELF_SERVICE_CONFIRMED", terminatedAt, auditEventId, true);
  }

  @Override
  public List<PrecheckSession> findSessionsByOwner(String ownerUserCode) {
    return jdbc.query(
        "SELECT * FROM precheck_session_v2 WHERE owner_user_code = ? ORDER BY updated_at DESC, session_id",
        (resultSet, rowNumber) -> session(resultSet),
        ownerUserCode);
  }

  @Override
  public List<PrecheckRun> findRuns(UUID sessionId) {
    return jdbc.query(
        "SELECT * FROM precheck_run_v2 WHERE session_id = ? ORDER BY sequence_number",
        (resultSet, rowNumber) -> run(resultSet),
        sessionId);
  }

  @Override
  public Optional<PrecheckRun> findRun(UUID sessionId, UUID runId) {
    return jdbc
        .query(
            "SELECT * FROM precheck_run_v2 WHERE session_id = ? AND run_id = ?",
            (resultSet, rowNumber) -> run(resultSet),
            sessionId,
            runId)
        .stream()
        .findFirst();
  }

  @Override
  public List<CompletenessPolicy> findPolicies(String issueTypeCode) {
    if (issueTypeCode == null || issueTypeCode.isBlank()) {
      return jdbc.query(
          "SELECT * FROM completeness_policy ORDER BY created_at DESC, policy_version, issue_type_code",
          (resultSet, rowNumber) -> policy(resultSet));
    }
    return jdbc.query(
        "SELECT * FROM completeness_policy WHERE issue_type_code = ? ORDER BY created_at DESC, policy_version",
        (resultSet, rowNumber) -> policy(resultSet),
        issueTypeCode);
  }

  @Override
  public Optional<CompletenessPolicy> findPolicyVersion(String policyVersion) {
    return jdbc
        .query(
            "SELECT * FROM completeness_policy WHERE policy_version = ? ORDER BY issue_type_code",
            (resultSet, rowNumber) -> policy(resultSet),
            policyVersion)
        .stream()
        .findFirst();
  }

  private PrecheckSession session(ResultSet resultSet) throws SQLException {
    var sessionId = resultSet.getObject("session_id", UUID.class);
    var runs =
        jdbc.query(
            "SELECT * FROM precheck_run_v2 WHERE session_id = ? ORDER BY sequence_number DESC",
            (runResult, rowNumber) -> run(runResult),
            sessionId);
    return new PrecheckSession(
        sessionId,
        resultSet.getString("owner_user_code"),
        resultSet.getString("status"),
        resultSet.getString("termination_reason"),
        runs.getFirst(),
        runs.size(),
        3,
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getTimestamp("updated_at").toInstant(),
        resultSet.getBoolean("mock_data"));
  }

  private CompletenessPolicy policy(ResultSet resultSet) throws SQLException {
    return new CompletenessPolicy(
        resultSet.getString("policy_version"),
        new IssueTypeValue(
            resultSet.getString("issue_type_code"), resultSet.getString("issue_type_display_name")),
        codes(resultSet.getString("general_field_codes")),
        codes(resultSet.getString("issue_specific_field_codes")),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getBoolean("mock_data"));
  }

  private PrecheckRun run(ResultSet resultSet) throws SQLException {
    try {
      return new PrecheckRun(
          resultSet.getObject("run_id", UUID.class),
          resultSet.getObject("session_id", UUID.class),
          resultSet.getInt("sequence_number"),
          resultSet.getString("status"),
          objectMapper.readValue(resultSet.getString("context_snapshot"), PrecheckContext.class),
          objectMapper.readValue(resultSet.getString("result_snapshot"), PrecheckResult.class),
          resultSet.getTimestamp("created_at").toInstant(),
          resultSet.getTimestamp("completed_at").toInstant());
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Stored precheck snapshot is invalid", exception);
    }
  }

  private PrecheckRun loadRun(UUID runId) {
    return jdbc.queryForObject(
        "SELECT * FROM precheck_run_v2 WHERE run_id = ?",
        (resultSet, rowNumber) -> run(resultSet),
        runId);
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Precheck snapshot is invalid", exception);
    }
  }

  private List<String> codes(String value) {
    return value.isBlank() ? List.of() : Arrays.stream(value.split(",")).toList();
  }
}
