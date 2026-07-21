package com.transwarp.serviceinsight.admin.reset.infrastructure.jdbc;

import com.transwarp.serviceinsight.admin.reset.domain.AdminResetModels.AdminReset;
import com.transwarp.serviceinsight.admin.reset.domain.AdminResetModels.IdempotencyRecord;
import com.transwarp.serviceinsight.admin.reset.domain.AdminResetModels.TaskError;
import com.transwarp.serviceinsight.admin.reset.port.AdminResetRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAdminResetRepository implements AdminResetRepository {
  private final JdbcTemplate jdbc;

  public JdbcAdminResetRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void lockIdempotency(String idempotencyKey) {
    jdbc.update(
        "INSERT INTO admin_reset_idempotency_lock(idempotency_key) VALUES (?) ON CONFLICT DO NOTHING",
        idempotencyKey);
    jdbc.queryForObject(
        "SELECT idempotency_key FROM admin_reset_idempotency_lock WHERE idempotency_key=? FOR UPDATE",
        String.class,
        idempotencyKey);
  }

  @Override
  public Optional<IdempotencyRecord> findIdempotency(String idempotencyKey) {
    return jdbc
        .query(
            "SELECT i.request_hash, r.* FROM admin_reset_idempotency i JOIN admin_reset r ON r.task_id=i.task_id WHERE i.idempotency_key=?",
            (resultSet, rowNumber) ->
                new IdempotencyRecord(
                    resultSet.getString("request_hash"), reset(resultSet, rowNumber)),
            idempotencyKey)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<AdminReset> findById(UUID taskId) {
    return jdbc.query("SELECT * FROM admin_reset WHERE task_id=?", this::reset, taskId).stream()
        .findFirst();
  }

  @Override
  public List<AdminReset> findAll() {
    return jdbc.query("SELECT * FROM admin_reset", this::reset);
  }

  @Override
  public void create(
      AdminReset reset,
      String reason,
      String idempotencyKey,
      String requestHash,
      Instant createdAt) {
    jdbc.update(
        "INSERT INTO admin_reset(task_id,resource_id,environment_code,confirmed_by,audit_event_id,reason,status,attempt,max_attempts,created_at,mock_data) VALUES (?,?,?,?,?,?,?,?,?,?,TRUE)",
        reset.taskId(),
        reset.resourceId(),
        reset.environmentCode(),
        reset.confirmedBy(),
        reset.auditEventId(),
        reason,
        reset.status(),
        reset.attempt(),
        reset.maxAttempts(),
        Timestamp.from(createdAt));
    jdbc.update(
        "INSERT INTO admin_reset_idempotency(idempotency_key,request_hash,task_id,created_at) VALUES (?,?,?,?)",
        idempotencyKey,
        requestHash,
        reset.taskId(),
        Timestamp.from(createdAt));
  }

  @Override
  public Optional<AdminReset> claim(UUID taskId, Instant startedAt) {
    var changed =
        jdbc.update(
            "UPDATE admin_reset SET status='RUNNING',attempt=attempt+1,started_at=?,error_code=NULL,error_message=NULL,error_retryable=NULL WHERE task_id=? AND status='PENDING'",
            Timestamp.from(startedAt),
            taskId);
    return changed == 0 ? Optional.empty() : findById(taskId);
  }

  @Override
  public void complete(UUID taskId, Instant completedAt) {
    jdbc.update(
        "UPDATE admin_reset SET status='SUCCEEDED',completed_at=?,next_retry_at=NULL WHERE task_id=? AND status='RUNNING'",
        Timestamp.from(completedAt),
        taskId);
  }

  @Override
  public void fail(UUID taskId, String code, String message, Instant completedAt) {
    jdbc.update(
        "UPDATE admin_reset SET status='FAILED',error_code=?,error_message=?,error_retryable=FALSE,completed_at=?,next_retry_at=NULL WHERE task_id=? AND status='RUNNING'",
        code,
        message,
        Timestamp.from(completedAt),
        taskId);
  }

  @Override
  public List<UUID> recoverIncomplete() {
    jdbc.update(
        "UPDATE admin_reset SET status='PENDING',started_at=NULL WHERE status='RUNNING' AND attempt<max_attempts");
    jdbc.update(
        "UPDATE admin_reset SET status='FAILED',error_code='RESET_ATTEMPTS_EXHAUSTED',error_message='本地模拟数据重置已耗尽三次尝试',error_retryable=FALSE,completed_at=CURRENT_TIMESTAMP WHERE status='RUNNING'");
    return jdbc.query(
        "SELECT task_id FROM admin_reset WHERE status='PENDING' ORDER BY created_at",
        (resultSet, rowNumber) -> resultSet.getObject("task_id", UUID.class));
  }

  private AdminReset reset(ResultSet resultSet, int rowNumber) throws SQLException {
    var errorCode = resultSet.getString("error_code");
    return new AdminReset(
        resultSet.getObject("task_id", UUID.class),
        resultSet.getObject("resource_id", UUID.class),
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
        resultSet.getString("environment_code"),
        resultSet.getString("confirmed_by"),
        resultSet.getObject("audit_event_id", UUID.class));
  }

  private Instant instant(ResultSet resultSet, String column) throws SQLException {
    var value = resultSet.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
