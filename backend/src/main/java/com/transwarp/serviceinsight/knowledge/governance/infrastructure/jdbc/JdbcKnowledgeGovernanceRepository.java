package com.transwarp.serviceinsight.knowledge.governance.infrastructure.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.CommandResult;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.DraftRevision;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.GovernanceCommand;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.IdempotencyRecord;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.NewRevision;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.RevisionCreated;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.Version;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.VersionState;
import com.transwarp.serviceinsight.knowledge.governance.port.KnowledgeGovernanceRepository;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.CatalogValue;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParseTask;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.TaskError;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcKnowledgeGovernanceRepository implements KnowledgeGovernanceRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;

  public JdbcKnowledgeGovernanceRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  @Override
  public void lockIdempotency(String commandType, String idempotencyKey) {
    jdbc.update(
        "INSERT INTO knowledge_governance_idempotency_lock(command_type, idempotency_key) VALUES (?, ?) ON CONFLICT DO NOTHING",
        commandType,
        idempotencyKey);
    jdbc.queryForObject(
        "SELECT command_type FROM knowledge_governance_idempotency_lock WHERE command_type = ? AND idempotency_key = ? FOR UPDATE",
        String.class,
        commandType,
        idempotencyKey);
  }

  @Override
  public Optional<IdempotencyRecord> findIdempotency(String commandType, String idempotencyKey) {
    return jdbc
        .query(
            "SELECT * FROM knowledge_governance_idempotency WHERE command_type = ? AND idempotency_key = ?",
            (resultSet, rowNumber) -> {
              var revisionId = resultSet.getObject("revision_id", UUID.class);
              if (revisionId != null) {
                return new IdempotencyRecord(
                    resultSet.getString("request_hash"),
                    loadRevisionCreated(revisionId, resultSet.getObject("task_id", UUID.class)),
                    null);
              }
              return new IdempotencyRecord(
                  resultSet.getString("request_hash"), null, commandSnapshot(resultSet));
            },
            commandType,
            idempotencyKey)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<VersionState> lockVersion(UUID versionId) {
    var locked =
        jdbc.query(
            "SELECT version_id FROM knowledge_version_v2 WHERE version_id = ? FOR UPDATE",
            (resultSet, rowNumber) -> resultSet.getObject("version_id", UUID.class),
            versionId);
    if (locked.isEmpty()) return Optional.empty();
    return jdbc
        .query(
            """
            SELECT v.*, d.product_line_code, r.revision_number AS draft_revision_number,
                   t.status AS parse_status, p.parse_result_hash
            FROM knowledge_version_v2 v
            JOIN knowledge_document d ON d.document_id = v.document_id
            JOIN knowledge_draft_revision r ON r.revision_id = v.current_draft_revision_id
            LEFT JOIN parse_task t ON t.draft_revision_id = r.revision_id
            LEFT JOIN knowledge_revision_parse_result p ON p.task_id = t.task_id
            WHERE v.version_id = ?
            ORDER BY t.created_at DESC
            LIMIT 1
            """,
            (resultSet, rowNumber) ->
                new VersionState(
                    version(resultSet),
                    resultSet.getString("created_by"),
                    resultSet.getString("product_line_code"),
                    resultSet.getObject("current_draft_revision_id", UUID.class),
                    resultSet.getInt("draft_revision_number"),
                    resultSet.getString("parse_status"),
                    resultSet.getString("parse_result_hash"),
                    warningCodes(resultSet.getObject("current_draft_revision_id", UUID.class))),
            versionId)
        .stream()
        .findFirst();
  }

  @Override
  public RevisionCreated createRevision(NewRevision value) {
    jdbc.update(
        "INSERT INTO knowledge_draft_revision(revision_id, version_id, revision_number, title, product_line_code, product_line_display_name, created_at, cleaned_text, cleaned_text_hash, parse_warning_notes, created_by, mock_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)",
        value.revisionId(),
        value.versionId(),
        value.revisionNumber(),
        value.title(),
        value.productLine().code(),
        value.productLine().displayName(),
        Timestamp.from(value.occurredAt()),
        value.cleanedText(),
        value.cleanedTextHash(),
        json(value.parseWarningNotes()),
        value.actor());
    jdbc.update(
        "UPDATE knowledge_version_v2 SET current_draft_revision_id = ?, updated_at = ? WHERE version_id = ?",
        value.revisionId(),
        Timestamp.from(value.occurredAt()),
        value.versionId());
    jdbc.update(
        "UPDATE knowledge_document SET title = ?, product_line_code = ?, product_line_display_name = ? WHERE document_id = (SELECT document_id FROM knowledge_version_v2 WHERE version_id = ?)",
        value.title(),
        value.productLine().code(),
        value.productLine().displayName(),
        value.versionId());
    jdbc.update(
        "INSERT INTO parse_task(task_id, resource_id, status, attempt, max_attempts, created_at, draft_revision_id) VALUES (?, ?, 'PENDING', 0, 3, ?, ?)",
        value.taskId(),
        value.versionId(),
        Timestamp.from(value.occurredAt()),
        value.revisionId());
    var auditEventId = UUID.randomUUID();
    insertAudit(
        auditEventId,
        value.actor(),
        "KNOWLEDGE_DRAFT_REVISED",
        value.versionId(),
        null,
        Set.of(),
        value.occurredAt());
    jdbc.update(
        "INSERT INTO knowledge_governance_idempotency(command_type, idempotency_key, request_hash, version_id, revision_id, task_id, audit_event_id, created_at) VALUES ('REVISION', ?, ?, ?, ?, ?, ?, ?)",
        value.idempotencyKey(),
        value.requestHash(),
        value.versionId(),
        value.revisionId(),
        value.taskId(),
        auditEventId,
        Timestamp.from(value.occurredAt()));
    return loadRevisionCreated(value.revisionId(), value.taskId());
  }

  @Override
  public CommandResult apply(GovernanceCommand value) {
    jdbc.update(
        "UPDATE knowledge_version_v2 SET status = ?, submitted_by = ?, approved_by = ?, updated_at = ? WHERE version_id = ?",
        value.targetStatus(),
        value.submittedBy(),
        value.approvedBy(),
        Timestamp.from(value.occurredAt()),
        value.versionId());
    jdbc.update(
        "INSERT INTO knowledge_review_history(review_record_id, version_id, revision_id, action, actor_user_code, parse_result_hash, acknowledged_warning_codes, reason, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        value.versionId(),
        value.revisionId(),
        value.commandType(),
        value.actor(),
        value.parseResultHash(),
        warningCodes(value.acknowledgedWarningCodes()),
        value.reason(),
        Timestamp.from(value.occurredAt()));
    insertAudit(
        value.auditEventId(),
        value.actor(),
        auditAction(value.commandType()),
        value.versionId(),
        value.parseResultHash(),
        value.acknowledgedWarningCodes(),
        value.occurredAt());
    jdbc.update(
        "INSERT INTO knowledge_governance_idempotency(command_type, idempotency_key, request_hash, version_id, audit_event_id, result_status, result_submitted_by, result_approved_by, result_updated_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        value.commandType(),
        value.idempotencyKey(),
        value.requestHash(),
        value.versionId(),
        value.auditEventId(),
        value.targetStatus(),
        value.submittedBy(),
        value.approvedBy(),
        Timestamp.from(value.occurredAt()),
        Timestamp.from(value.occurredAt()));
    return new CommandResult(loadVersion(value.versionId()), value.auditEventId());
  }

  private RevisionCreated loadRevisionCreated(UUID revisionId, UUID taskId) {
    var revision =
        jdbc.queryForObject(
            "SELECT r.* FROM knowledge_draft_revision r WHERE r.revision_id = ?",
            (resultSet, rowNumber) -> revision(resultSet),
            revisionId);
    var task =
        jdbc.queryForObject(
            "SELECT * FROM parse_task WHERE task_id = ?",
            (resultSet, rowNumber) -> task(resultSet),
            taskId);
    return new RevisionCreated(revision, task);
  }

  private CommandResult commandSnapshot(ResultSet resultSet) throws SQLException {
    var versionId = resultSet.getObject("version_id", UUID.class);
    var current = loadVersion(versionId);
    var snapshot =
        new Version(
            current.versionId(),
            current.documentId(),
            current.revisionNumber(),
            resultSet.getString("result_status"),
            resultSet.getString("result_submitted_by"),
            resultSet.getString("result_approved_by"),
            current.createdAt(),
            resultSet.getTimestamp("result_updated_at").toInstant(),
            current.mockData());
    return new CommandResult(snapshot, resultSet.getObject("audit_event_id", UUID.class));
  }

  private Version loadVersion(UUID versionId) {
    return jdbc.queryForObject(
        "SELECT * FROM knowledge_version_v2 WHERE version_id = ?",
        (resultSet, rowNumber) -> version(resultSet),
        versionId);
  }

  private Version version(ResultSet resultSet) throws SQLException {
    return new Version(
        resultSet.getObject("version_id", UUID.class),
        resultSet.getObject("document_id", UUID.class),
        resultSet.getInt("revision_number"),
        resultSet.getString("status"),
        resultSet.getString("submitted_by"),
        resultSet.getString("approved_by"),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getTimestamp("updated_at").toInstant(),
        resultSet.getBoolean("mock_data"));
  }

  private DraftRevision revision(ResultSet resultSet) throws SQLException {
    return new DraftRevision(
        resultSet.getObject("revision_id", UUID.class),
        resultSet.getObject("version_id", UUID.class),
        resultSet.getInt("revision_number"),
        resultSet.getString("title"),
        new CatalogValue(
            resultSet.getString("product_line_code"),
            resultSet.getString("product_line_display_name")),
        resultSet.getString("cleaned_text_hash"),
        notes(resultSet.getString("parse_warning_notes")),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getBoolean("mock_data"));
  }

  private ParseTask task(ResultSet resultSet) throws SQLException {
    var errorCode = resultSet.getString("error_code");
    return new ParseTask(
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
        true);
  }

  private Set<String> warningCodes(UUID revisionId) {
    return new LinkedHashSet<>(
        jdbc.queryForList(
            "SELECT DISTINCT warning_code FROM knowledge_revision_parse_warning WHERE revision_id = ? ORDER BY warning_code",
            String.class,
            revisionId));
  }

  private void insertAudit(
      UUID eventId,
      String actor,
      String action,
      UUID versionId,
      String parseResultHash,
      Set<String> acknowledgedWarnings,
      Instant occurredAt) {
    jdbc.update(
        "INSERT INTO knowledge_governance_audit(event_id, actor_user_code, action, subject_id, outcome, parse_result_hash, acknowledged_warning_codes, occurred_at, mock_data) VALUES (?, ?, ?, ?, 'SUCCEEDED', ?, ?, ?, TRUE)",
        eventId,
        actor,
        action,
        versionId,
        parseResultHash,
        warningCodes(acknowledgedWarnings),
        Timestamp.from(occurredAt));
  }

  private String auditAction(String commandType) {
    return switch (commandType) {
      case "SUBMIT" -> "KNOWLEDGE_VERSION_REVIEW_SUBMITTED";
      case "RETURN" -> "KNOWLEDGE_VERSION_RETURNED_TO_DRAFT";
      case "APPROVE" -> "KNOWLEDGE_VERSION_APPROVED";
      default -> throw new IllegalArgumentException("Unknown command type");
    };
  }

  private String warningCodes(Set<String> values) {
    return values.stream().sorted().reduce((left, right) -> left + "," + right).orElse("");
  }

  private String json(List<String> values) {
    try {
      return objectMapper.writeValueAsString(values);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Invalid revision notes", exception);
    }
  }

  private List<String> notes(String value) {
    try {
      return objectMapper.readValue(value, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Stored revision notes are invalid", exception);
    }
  }

  private Instant instant(ResultSet resultSet, String column) throws SQLException {
    var value = resultSet.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
