package com.transwarp.serviceinsight.evaluation.infrastructure;

import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.EvaluationCaseResult;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.EvaluationRun;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.EvaluationSummary;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.IdempotencyRecord;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.StoredFailure;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.TaskError;
import com.transwarp.serviceinsight.evaluation.port.EvaluationRunRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcEvaluationRunRepository implements EvaluationRunRepository {
  private final JdbcTemplate jdbc;

  public JdbcEvaluationRunRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void lockIdempotency(String idempotencyKey) {
    jdbc.update(
        "INSERT INTO evaluation_run_idempotency_lock(idempotency_key) VALUES (?) ON CONFLICT DO NOTHING",
        idempotencyKey);
    jdbc.queryForObject(
        "SELECT idempotency_key FROM evaluation_run_idempotency_lock WHERE idempotency_key = ? FOR UPDATE",
        String.class,
        idempotencyKey);
  }

  @Override
  public Optional<IdempotencyRecord> findIdempotency(String idempotencyKey) {
    return jdbc
        .query(
            "SELECT i.request_hash, r.* FROM evaluation_run_idempotency i JOIN evaluation_run r ON r.task_id=i.task_id WHERE i.idempotency_key = ?",
            (resultSet, rowNumber) ->
                new IdempotencyRecord(resultSet.getString("request_hash"), run(resultSet)),
            idempotencyKey)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<EvaluationRun> findById(java.util.UUID taskId) {
    return jdbc
        .query("SELECT * FROM evaluation_run WHERE task_id = ?", (rs, row) -> run(rs), taskId)
        .stream()
        .findFirst();
  }

  @Override
  public void create(
      EvaluationRun run,
      String note,
      String requestedBy,
      String idempotencyKey,
      String requestHash,
      Instant createdAt) {
    jdbc.update(
        "INSERT INTO evaluation_run(task_id, resource_id, evaluation_set_version, note, requested_by, status, attempt, max_attempts, created_at, mock_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)",
        run.taskId(),
        run.resourceId(),
        run.evaluationSetVersion(),
        note,
        requestedBy,
        run.status(),
        run.attempt(),
        run.maxAttempts(),
        Timestamp.from(createdAt));
    jdbc.update(
        "INSERT INTO evaluation_run_idempotency(idempotency_key, request_hash, task_id, created_at) VALUES (?, ?, ?, ?)",
        idempotencyKey,
        requestHash,
        run.taskId(),
        Timestamp.from(createdAt));
  }

  @Override
  public Optional<EvaluationRun> claim(java.util.UUID taskId, Instant startedAt) {
    var changed =
        jdbc.update(
            "UPDATE evaluation_run SET status='RUNNING', attempt=attempt+1, started_at=? WHERE task_id=? AND status='PENDING'",
            Timestamp.from(startedAt),
            taskId);
    return changed == 0 ? Optional.empty() : findById(taskId);
  }

  @Override
  public void complete(
      java.util.UUID taskId,
      EvaluationSummary summary,
      List<EvaluationCaseResult> results,
      Instant completedAt) {
    for (var result : results) {
      jdbc.update(
          "INSERT INTO evaluation_case_result(task_id, case_id, scenario_tags, failed_checks, failure_codes, expected_summary, actual_summary, passed, trace_session_id, trace_run_id, mock_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)",
          taskId,
          result.caseId(),
          String.join(",", result.scenarioTags()),
          String.join(",", result.failedChecks()),
          String.join(",", result.failureCodes()),
          result.expectedSummary(),
          result.actualSummary(),
          result.passed(),
          result.traceSessionId(),
          result.traceRunId());
    }
    jdbc.update(
        "UPDATE evaluation_run SET status='SUCCEEDED', completed_at=?, summary_sample_count=?, summary_gate_passed=?, summary_permission_leakage_rate=?, summary_citation_error_rate=?, summary_degradation_pass_rate=?, summary_recall_at_5=? WHERE task_id=? AND status='RUNNING'",
        Timestamp.from(completedAt),
        summary.sampleCount(),
        summary.gatePassed(),
        summary.permissionLeakageRate(),
        summary.citationErrorRate(),
        summary.degradationPassRate(),
        summary.recallAt5(),
        taskId);
  }

  @Override
  public void fail(java.util.UUID taskId, String code, String message, Instant completedAt) {
    jdbc.update(
        "UPDATE evaluation_run SET status='FAILED', error_code=?, error_message=?, error_retryable=FALSE, completed_at=? WHERE task_id=? AND status='RUNNING'",
        code,
        message,
        Timestamp.from(completedAt),
        taskId);
  }

  @Override
  public List<EvaluationRun> findAll() {
    return jdbc.query("SELECT * FROM evaluation_run", (rs, row) -> run(rs));
  }

  @Override
  public List<StoredFailure> findFailures(java.util.UUID taskId) {
    return jdbc.query(
        "SELECT * FROM evaluation_case_result WHERE task_id=? AND passed=FALSE",
        (rs, row) ->
            new StoredFailure(
                rs.getString("case_id"), split(rs.getString("scenario_tags")),
                split(rs.getString("failed_checks")), split(rs.getString("failure_codes")),
                rs.getString("expected_summary"), rs.getString("actual_summary")),
        taskId);
  }

  @Override
  public List<java.util.UUID> recoverIncomplete() {
    jdbc.update(
        "UPDATE evaluation_run SET status='PENDING', started_at=NULL WHERE status='RUNNING' AND attempt < max_attempts");
    return jdbc.query(
        "SELECT task_id FROM evaluation_run WHERE status='PENDING' ORDER BY created_at",
        (rs, row) -> rs.getObject("task_id", java.util.UUID.class));
  }

  private List<String> split(String value) {
    return value == null || value.isBlank() ? List.of() : java.util.Arrays.asList(value.split(","));
  }

  private EvaluationRun run(ResultSet resultSet) throws SQLException {
    var errorCode = resultSet.getString("error_code");
    var sampleCount = resultSet.getObject("summary_sample_count", Integer.class);
    return new EvaluationRun(
        resultSet.getObject("task_id", java.util.UUID.class),
        resultSet.getObject("resource_id", java.util.UUID.class),
        resultSet.getString("status"),
        resultSet.getInt("attempt"),
        resultSet.getInt("max_attempts"),
        errorCode == null
            ? null
            : new TaskError(
                errorCode,
                resultSet.getString("error_message"),
                resultSet.getBoolean("error_retryable")),
        instant(resultSet, "next_retry_at"),
        resultSet.getTimestamp("created_at").toInstant(),
        instant(resultSet, "started_at"),
        instant(resultSet, "completed_at"),
        resultSet.getBoolean("mock_data"),
        resultSet.getString("evaluation_set_version"),
        sampleCount == null
            ? null
            : new EvaluationSummary(
                sampleCount,
                resultSet.getBoolean("summary_gate_passed"),
                resultSet.getDouble("summary_permission_leakage_rate"),
                resultSet.getDouble("summary_citation_error_rate"),
                resultSet.getDouble("summary_degradation_pass_rate"),
                resultSet.getDouble("summary_recall_at_5"),
                com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.DISCLAIMER));
  }

  private Instant instant(ResultSet resultSet, String column) throws SQLException {
    var value = resultSet.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
