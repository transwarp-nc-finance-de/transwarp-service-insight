package com.transwarp.serviceinsight.precheck.interaction.infrastructure.jdbc;

import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.ContinuationIdempotency;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.ContinuationRecord;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.Feedback;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.FeedbackIdempotency;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.FeedbackRecord;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.SubmissionContinuation;
import com.transwarp.serviceinsight.precheck.interaction.port.InteractionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInteractionRepository implements InteractionRepository {
  private final JdbcTemplate jdbc;

  public JdbcInteractionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<FeedbackIdempotency> findFeedbackIdempotency(String idempotencyKey) {
    return jdbc
        .query(
            "SELECT i.request_hash, f.* FROM precheck_feedback_idempotency i JOIN precheck_feedback_v2 f ON f.feedback_id=i.feedback_id WHERE i.idempotency_key = ?",
            (resultSet, rowNumber) ->
                new FeedbackIdempotency(resultSet.getString("request_hash"), feedback(resultSet)),
            idempotencyKey)
        .stream()
        .findFirst();
  }

  @Override
  public void saveFeedback(FeedbackRecord record, String idempotencyKey, String requestHash) {
    var feedback = record.feedback();
    jdbc.update(
        "INSERT INTO precheck_feedback_v2(feedback_id, session_id, run_id, owner_user_code, product_line_code, adoption_status, helpfulness, reason, recorded_at, mock_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)",
        feedback.feedbackId(),
        feedback.sessionId(),
        feedback.runId(),
        record.ownerUserCode(),
        record.productLineCode(),
        feedback.adoptionStatus(),
        feedback.helpfulness(),
        feedback.reason(),
        Timestamp.from(feedback.recordedAt()));
    jdbc.update(
        "INSERT INTO precheck_feedback_idempotency(idempotency_key, request_hash, feedback_id, created_at) VALUES (?, ?, ?, ?)",
        idempotencyKey,
        requestHash,
        feedback.feedbackId(),
        Timestamp.from(feedback.recordedAt()));
  }

  @Override
  public Optional<FeedbackRecord> findFeedback(UUID feedbackId) {
    return jdbc
        .query(
            "SELECT * FROM precheck_feedback_v2 WHERE feedback_id = ?",
            (resultSet, rowNumber) -> feedback(resultSet),
            feedbackId)
        .stream()
        .findFirst();
  }

  private FeedbackRecord feedback(ResultSet resultSet) throws SQLException {
    return new FeedbackRecord(
        new Feedback(
            resultSet.getObject("feedback_id", UUID.class),
            resultSet.getObject("session_id", UUID.class),
            resultSet.getObject("run_id", UUID.class),
            resultSet.getString("adoption_status"),
            resultSet.getString("helpfulness"),
            resultSet.getString("reason"),
            resultSet.getTimestamp("recorded_at").toInstant(),
            resultSet.getBoolean("mock_data")),
        resultSet.getString("owner_user_code"),
        resultSet.getString("product_line_code"));
  }

  @Override
  public Optional<ContinuationIdempotency> findContinuationIdempotency(String idempotencyKey) {
    return jdbc
        .query(
            "SELECT i.request_hash, c.* FROM submission_continuation_idempotency i JOIN submission_continuation_v2 c ON c.continuation_id=i.continuation_id WHERE i.idempotency_key = ?",
            (resultSet, rowNumber) ->
                new ContinuationIdempotency(
                    resultSet.getString("request_hash"), continuation(resultSet)),
            idempotencyKey)
        .stream()
        .findFirst();
  }

  @Override
  public void saveContinuation(
      ContinuationRecord record, String idempotencyKey, String requestHash) {
    var continuation = record.continuation();
    jdbc.update(
        "INSERT INTO submission_continuation_v2(continuation_id, session_id, confirmed_by, product_line_code, confirmed_at, reason, audit_event_id, mock_data) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)",
        continuation.continuationId(),
        continuation.sessionId(),
        continuation.confirmedBy(),
        record.productLineCode(),
        Timestamp.from(continuation.confirmedAt()),
        continuation.reason(),
        continuation.auditEventId());
    jdbc.update(
        "UPDATE precheck_session_v2 SET status = 'TERMINATED', termination_reason = 'CONTINUED_SUBMISSION', updated_at = ? WHERE session_id = ?",
        Timestamp.from(continuation.confirmedAt()),
        continuation.sessionId());
    jdbc.update(
        "INSERT INTO submission_continuation_idempotency(idempotency_key, request_hash, continuation_id, created_at) VALUES (?, ?, ?, ?)",
        idempotencyKey,
        requestHash,
        continuation.continuationId(),
        Timestamp.from(continuation.confirmedAt()));
  }

  @Override
  public Optional<ContinuationRecord> findContinuation(UUID continuationId) {
    return jdbc
        .query(
            "SELECT * FROM submission_continuation_v2 WHERE continuation_id = ?",
            (resultSet, rowNumber) -> continuation(resultSet),
            continuationId)
        .stream()
        .findFirst();
  }

  private ContinuationRecord continuation(ResultSet resultSet) throws SQLException {
    return new ContinuationRecord(
        new SubmissionContinuation(
            resultSet.getObject("continuation_id", UUID.class),
            resultSet.getObject("session_id", UUID.class),
            resultSet.getString("confirmed_by"),
            resultSet.getTimestamp("confirmed_at").toInstant(),
            resultSet.getString("reason"),
            resultSet.getObject("audit_event_id", UUID.class),
            resultSet.getBoolean("mock_data")),
        resultSet.getString("product_line_code"));
  }
}
