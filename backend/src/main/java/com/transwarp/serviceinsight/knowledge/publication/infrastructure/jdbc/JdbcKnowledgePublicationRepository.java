package com.transwarp.serviceinsight.knowledge.publication.infrastructure.jdbc;

import com.transwarp.serviceinsight.knowledge.publication.domain.IndexTask;
import com.transwarp.serviceinsight.knowledge.publication.domain.IndexTask.TaskError;
import com.transwarp.serviceinsight.knowledge.publication.port.KnowledgePublicationRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcKnowledgePublicationRepository implements KnowledgePublicationRepository {
  private final JdbcTemplate jdbc;
  private final boolean postgresql;

  public JdbcKnowledgePublicationRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
    this.postgresql =
        Boolean.TRUE.equals(
            jdbc.execute(
                (ConnectionCallback<Boolean>)
                    connection ->
                        "PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())));
  }

  @Override
  public void lockIdempotency(String commandType, String idempotencyKey) {
    jdbc.update(
        "INSERT INTO knowledge_publication_idempotency_lock(command_type, idempotency_key) VALUES (?, ?) ON CONFLICT DO NOTHING",
        commandType,
        idempotencyKey);
    jdbc.queryForObject(
        "SELECT command_type FROM knowledge_publication_idempotency_lock WHERE command_type = ? AND idempotency_key = ? FOR UPDATE",
        String.class,
        commandType,
        idempotencyKey);
  }

  @Override
  public Optional<IdempotencyRecord> findIdempotency(String commandType, String idempotencyKey) {
    return jdbc
        .query(
            "SELECT request_hash, task_id FROM knowledge_publication_idempotency WHERE command_type = ? AND idempotency_key = ?",
            (rs, row) ->
                new IdempotencyRecord(
                    rs.getString("request_hash"), loadTask(rs.getObject("task_id", UUID.class))),
            commandType,
            idempotencyKey)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<ApprovedVersion> lockApprovedVersion(UUID versionId) {
    return jdbc
        .query(
            """
            SELECT v.version_id, v.current_draft_revision_id, d.product_line_code
            FROM knowledge_version_v2 v
            JOIN knowledge_document d ON d.document_id = v.document_id
            WHERE v.version_id = ? AND v.status = 'APPROVED'
            FOR UPDATE
            """,
            (rs, row) ->
                new ApprovedVersion(
                    rs.getObject("version_id", UUID.class),
                    rs.getObject("current_draft_revision_id", UUID.class),
                    rs.getString("product_line_code")),
            versionId)
        .stream()
        .findFirst();
  }

  @Override
  public boolean hasActiveTask(UUID versionId) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM index_task WHERE resource_id = ? AND status IN ('PENDING', 'RUNNING')",
            Integer.class,
            versionId)
        > 0;
  }

  @Override
  public IndexTask createTask(
      UUID taskId,
      ApprovedVersion version,
      String actor,
      String idempotencyKey,
      String requestHash,
      Instant createdAt) {
    jdbc.update(
        "INSERT INTO index_task(task_id, resource_id, revision_id, actor_user_code, status, fts_status, embedding_status, attempt, max_attempts, created_at) VALUES (?, ?, ?, ?, 'PENDING', 'PENDING', 'PENDING', 0, 3, ?)",
        taskId,
        version.versionId(),
        version.revisionId(),
        actor,
        Timestamp.from(createdAt));
    jdbc.update(
        "INSERT INTO knowledge_publication_idempotency(command_type, idempotency_key, request_hash, version_id, task_id, created_at) VALUES ('PUBLISH', ?, ?, ?, ?, ?)",
        idempotencyKey,
        requestHash,
        version.versionId(),
        taskId,
        Timestamp.from(createdAt));
    return loadTask(taskId);
  }

  @Override
  @Transactional
  public Optional<IndexInput> claimPendingTask(UUID taskId, Instant startedAt) {
    var updated =
        jdbc.update(
            "UPDATE index_task SET status = 'RUNNING', fts_status = 'RUNNING', embedding_status = 'RUNNING', attempt = attempt + 1, started_at = ?, next_retry_at = NULL WHERE task_id = ? AND status = 'PENDING' AND attempt < max_attempts AND (next_retry_at IS NULL OR next_retry_at <= ?)",
            Timestamp.from(startedAt),
            taskId,
            Timestamp.from(startedAt));
    if (updated == 0) return Optional.empty();
    return jdbc
        .query(
            """
            SELECT t.*, v.document_id, d.product_line_code
            FROM index_task t
            JOIN knowledge_version_v2 v ON v.version_id = t.resource_id
            JOIN knowledge_document d ON d.document_id = v.document_id
            WHERE t.task_id = ?
            """,
            (rs, row) ->
                new IndexInput(
                    rs.getObject("task_id", UUID.class),
                    rs.getObject("resource_id", UUID.class),
                    rs.getObject("revision_id", UUID.class),
                    rs.getObject("document_id", UUID.class),
                    rs.getString("product_line_code"),
                    rs.getString("actor_user_code"),
                    rs.getInt("attempt"),
                    rs.getInt("max_attempts"),
                    loadChunks(rs.getObject("revision_id", UUID.class))),
            taskId)
        .stream()
        .findFirst();
  }

  @Override
  public void markFtsPrepared(IndexInput input) {
    for (var chunk : input.chunks()) {
      if (postgresql) {
        jdbc.update(
            "INSERT INTO knowledge_chunk_fts_stage(task_id, chunk_id, text_content) VALUES (?, ?, ?) ON CONFLICT (task_id, chunk_id) DO UPDATE SET text_content = EXCLUDED.text_content",
            input.taskId(),
            chunk.chunkId(),
            chunk.text());
      } else {
        jdbc.update(
            "MERGE INTO knowledge_chunk_fts_stage(task_id, chunk_id, text_content, fts_document) KEY(task_id, chunk_id) VALUES (?, ?, ?, ?)",
            input.taskId(),
            chunk.chunkId(),
            chunk.text(),
            chunk.text().toLowerCase(java.util.Locale.ROOT));
      }
    }
    jdbc.update(
        "UPDATE index_task SET fts_status = 'SUCCEEDED', embedding_status = 'RUNNING' WHERE task_id = ? AND status = 'RUNNING'",
        input.taskId());
  }

  @Override
  @Transactional
  public void completeTask(IndexInput input, List<float[]> embeddings, Instant completedAt) {
    if (input.chunks().size() != embeddings.size()) {
      throw new IllegalArgumentException("embedding count does not match chunk count");
    }
    for (int index = 0; index < input.chunks().size(); index++) {
      var chunk = input.chunks().get(index);
      var embedding = embeddings.get(index);
      if (embedding.length != 768) {
        throw new IllegalArgumentException("embedding dimension must be 768");
      }
      insertIndex(input, chunk, embedding, completedAt);
    }
    var previous =
        jdbc.queryForObject(
            "SELECT current_published_version_id FROM knowledge_document WHERE document_id = ? FOR UPDATE",
            UUID.class,
            input.documentId());
    if (previous != null && !previous.equals(input.versionId())) {
      jdbc.update(
          "UPDATE knowledge_version_v2 SET status = 'DEPRECATED', updated_at = ? WHERE version_id = ? AND status = 'PUBLISHED'",
          Timestamp.from(completedAt),
          previous);
    }
    var published =
        jdbc.update(
            "UPDATE knowledge_version_v2 SET status = 'PUBLISHED', updated_at = ? WHERE version_id = ? AND status = 'APPROVED' AND current_draft_revision_id = ?",
            Timestamp.from(completedAt),
            input.versionId(),
            input.revisionId());
    if (published != 1) {
      throw new IllegalStateException("approved version changed while indexing");
    }
    jdbc.update(
        "UPDATE knowledge_document SET current_published_version_id = ? WHERE document_id = ?",
        input.versionId(),
        input.documentId());
    jdbc.update(
        "UPDATE index_task SET status = 'SUCCEEDED', fts_status = 'SUCCEEDED', embedding_status = 'SUCCEEDED', error_code = NULL, error_message = NULL, error_retryable = NULL, completed_at = ? WHERE task_id = ? AND status = 'RUNNING'",
        Timestamp.from(completedAt),
        input.taskId());
    jdbc.update(
        "INSERT INTO knowledge_governance_audit(event_id, actor_user_code, action, subject_id, outcome, parse_result_hash, acknowledged_warning_codes, occurred_at, mock_data) VALUES (?, ?, 'KNOWLEDGE_VERSION_PUBLISHED', ?, 'SUCCEEDED', NULL, '', ?, TRUE)",
        UUID.randomUUID(),
        input.actorUserCode(),
        input.versionId(),
        Timestamp.from(completedAt));
    insertAttempt(input, "SUCCEEDED", "SUCCEEDED", "SUCCEEDED", null, null, completedAt);
    jdbc.update("DELETE FROM knowledge_chunk_fts_stage WHERE task_id = ?", input.taskId());
  }

  @Override
  @Transactional
  public void scheduleRetry(
      IndexInput input,
      String failedBranch,
      String errorCode,
      String safeMessage,
      Instant failedAt,
      Instant nextRetryAt) {
    jdbc.update(
        "UPDATE index_task SET status = 'PENDING', fts_status = 'PENDING', embedding_status = 'PENDING', error_code = ?, error_message = ?, error_retryable = TRUE, next_retry_at = ? WHERE task_id = ? AND status = 'RUNNING'",
        errorCode,
        safeMessage,
        Timestamp.from(nextRetryAt),
        input.taskId());
    insertAttempt(
        input,
        "FAILED",
        "FTS".equals(failedBranch) ? "FAILED" : "SUCCEEDED",
        "EMBEDDING".equals(failedBranch) ? "FAILED" : "SUCCEEDED",
        errorCode,
        true,
        failedAt);
  }

  @Override
  @Transactional
  public void failTask(
      IndexInput input,
      String failedBranch,
      String errorCode,
      String safeMessage,
      boolean retryable,
      Instant completedAt) {
    var ftsStatus =
        "FTS".equals(failedBranch) || "INDEX".equals(failedBranch) ? "FAILED" : "SUCCEEDED";
    var embeddingStatus =
        "EMBEDDING".equals(failedBranch) || "INDEX".equals(failedBranch) ? "FAILED" : "SUCCEEDED";
    jdbc.update(
        "UPDATE index_task SET status = 'FAILED', fts_status = ?, embedding_status = ?, error_code = ?, error_message = ?, error_retryable = ?, next_retry_at = NULL, completed_at = ? WHERE task_id = ? AND status = 'RUNNING'",
        ftsStatus,
        embeddingStatus,
        errorCode,
        safeMessage,
        retryable,
        Timestamp.from(completedAt),
        input.taskId());
    insertAttempt(input, "FAILED", ftsStatus, embeddingStatus, errorCode, retryable, completedAt);
    jdbc.update("DELETE FROM knowledge_chunk_fts_stage WHERE task_id = ?", input.taskId());
  }

  @Override
  @Transactional
  public List<RecoveryTask> recoverIncompleteTasks(Instant recoveredAt) {
    jdbc.update(
        "INSERT INTO index_task_attempt(task_id, attempt, status, fts_status, embedding_status, error_code, error_retryable, started_at, completed_at) SELECT task_id, attempt, 'FAILED', fts_status, embedding_status, CASE WHEN attempt >= max_attempts THEN 'INDEX_RESTART_ATTEMPTS_EXHAUSTED' ELSE 'INDEX_INTERRUPTED' END, CASE WHEN attempt >= max_attempts THEN FALSE ELSE TRUE END, started_at, ? FROM index_task WHERE status = 'RUNNING'",
        Timestamp.from(recoveredAt));
    jdbc.update(
        "UPDATE index_task SET status = 'FAILED', fts_status = 'FAILED', embedding_status = 'FAILED', error_code = 'INDEX_RESTART_ATTEMPTS_EXHAUSTED', error_message = 'Indexing was interrupted after the final attempt.', error_retryable = FALSE, next_retry_at = NULL, completed_at = ? WHERE status = 'RUNNING' AND attempt >= max_attempts",
        Timestamp.from(recoveredAt));
    jdbc.update(
        "DELETE FROM knowledge_chunk_fts_stage WHERE task_id IN (SELECT task_id FROM index_task WHERE error_code = 'INDEX_RESTART_ATTEMPTS_EXHAUSTED' AND completed_at = ?)",
        Timestamp.from(recoveredAt));
    jdbc.update(
        "UPDATE index_task SET status = 'PENDING', fts_status = 'PENDING', embedding_status = 'PENDING', error_code = 'INDEX_INTERRUPTED', error_message = 'Indexing was interrupted and will be retried.', error_retryable = TRUE, next_retry_at = ? WHERE status = 'RUNNING' AND attempt < max_attempts",
        Timestamp.from(recoveredAt));
    return jdbc.query(
        "SELECT task_id, COALESCE(next_retry_at, ?) AS effective_retry_at FROM index_task WHERE status = 'PENDING' AND attempt < max_attempts ORDER BY created_at, task_id",
        (rs, row) ->
            new RecoveryTask(
                rs.getObject("task_id", UUID.class),
                rs.getTimestamp("effective_retry_at").toInstant()),
        Timestamp.from(recoveredAt));
  }

  @Override
  public Optional<IndexTask> findVisibleTask(
      UUID taskId, String userCode, List<String> productLines) {
    var sql =
        """
        SELECT t.* FROM index_task t
        JOIN knowledge_version_v2 v ON v.version_id = t.resource_id
        JOIN knowledge_document d ON d.document_id = v.document_id
        WHERE t.task_id = ? AND d.product_line_code IN (%s)
        """
            .formatted(String.join(",", Collections.nCopies(productLines.size(), "?")));
    var parameters = new java.util.ArrayList<>();
    parameters.add(taskId);
    parameters.addAll(productLines);
    return jdbc.query(sql, (rs, row) -> task(rs), parameters.toArray()).stream().findFirst();
  }

  @Override
  public TaskPage listVisibleTasks(
      List<String> productLines,
      int page,
      int size,
      String sortDirection,
      String sortBy,
      String status,
      UUID versionId) {
    var filters =
        new StringBuilder("d.product_line_code IN (")
            .append(String.join(",", Collections.nCopies(productLines.size(), "?")))
            .append(")");
    var parameters = new java.util.ArrayList<>();
    parameters.addAll(productLines);
    if (status != null) {
      filters.append(" AND t.status = ?");
      parameters.add(status);
    }
    if (versionId != null) {
      filters.append(" AND t.resource_id = ?");
      parameters.add(versionId);
    }
    var from =
        " FROM index_task t JOIN knowledge_version_v2 v ON v.version_id = t.resource_id"
            + " JOIN knowledge_document d ON d.document_id = v.document_id WHERE "
            + filters;
    var total = jdbc.queryForObject("SELECT COUNT(*)" + from, Long.class, parameters.toArray());
    var queryParameters = new java.util.ArrayList<>(parameters);
    queryParameters.add(size);
    queryParameters.add((page - 1) * size);
    var items =
        jdbc.query(
            "SELECT t.*"
                + from
                + " ORDER BY "
                + ("status".equals(sortBy) ? "t.status" : "t.created_at")
                + " "
                + sortDirection
                + ", t.task_id "
                + sortDirection
                + " LIMIT ? OFFSET ?",
            (rs, row) -> task(rs),
            queryParameters.toArray());
    return new TaskPage(
        items,
        new PageMetadata(
            page, size, total, (int) Math.ceil(total / (double) size), sortBy, sortDirection));
  }

  private List<IndexChunk> loadChunks(UUID revisionId) {
    return jdbc.query(
        "SELECT chunk_id, text_content, content_hash FROM knowledge_revision_chunk WHERE revision_id = ? ORDER BY sequence",
        (rs, row) ->
            new IndexChunk(
                rs.getObject("chunk_id", UUID.class),
                rs.getString("text_content"),
                rs.getString("content_hash")),
        revisionId);
  }

  private void insertIndex(
      IndexInput input, IndexChunk chunk, float[] embedding, Instant indexedAt) {
    var vector = vectorLiteral(embedding);
    if (postgresql) {
      jdbc.update(
          "INSERT INTO knowledge_chunk_index(chunk_id, version_id, revision_id, product_line_code, text_content, embedding, embedding_model, embedding_revision, content_hash, indexed_at) VALUES (?, ?, ?, ?, ?, CAST(? AS vector), 'intfloat/multilingual-e5-base', 'd13f1b27baf31030b7fd040960d60d909913633f', ?, ?)",
          chunk.chunkId(),
          input.versionId(),
          input.revisionId(),
          input.productLineCode(),
          chunk.text(),
          vector,
          chunk.contentHash(),
          Timestamp.from(indexedAt));
      return;
    }
    jdbc.update(
        "INSERT INTO knowledge_chunk_index(chunk_id, version_id, revision_id, product_line_code, text_content, fts_document, embedding, embedding_dimensions, embedding_model, embedding_revision, content_hash, indexed_at) VALUES (?, ?, ?, ?, ?, ?, ?, 768, 'intfloat/multilingual-e5-base', 'd13f1b27baf31030b7fd040960d60d909913633f', ?, ?)",
        chunk.chunkId(),
        input.versionId(),
        input.revisionId(),
        input.productLineCode(),
        chunk.text(),
        chunk.text().toLowerCase(java.util.Locale.ROOT),
        vector,
        chunk.contentHash(),
        Timestamp.from(indexedAt));
  }

  private String vectorLiteral(float[] values) {
    var result = new StringBuilder("[");
    for (int index = 0; index < values.length; index++) {
      if (index > 0) result.append(',');
      result.append(Float.toString(values[index]));
    }
    return result.append(']').toString();
  }

  private void insertAttempt(
      IndexInput input,
      String status,
      String ftsStatus,
      String embeddingStatus,
      String errorCode,
      Boolean retryable,
      Instant completedAt) {
    jdbc.update(
        "INSERT INTO index_task_attempt(task_id, attempt, status, fts_status, embedding_status, error_code, error_retryable, started_at, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, (SELECT started_at FROM index_task WHERE task_id = ?), ?)",
        input.taskId(),
        input.attempt(),
        status,
        ftsStatus,
        embeddingStatus,
        errorCode,
        retryable,
        input.taskId(),
        Timestamp.from(completedAt));
  }

  private IndexTask loadTask(UUID taskId) {
    return jdbc.queryForObject(
        "SELECT * FROM index_task WHERE task_id = ?", (rs, row) -> task(rs), taskId);
  }

  private IndexTask task(ResultSet rs) throws SQLException {
    var errorCode = rs.getString("error_code");
    return new IndexTask(
        rs.getObject("task_id", UUID.class),
        rs.getObject("resource_id", UUID.class),
        rs.getString("status"),
        rs.getInt("attempt"),
        rs.getInt("max_attempts"),
        errorCode == null
            ? null
            : new TaskError(
                errorCode, rs.getString("error_message"), rs.getBoolean("error_retryable")),
        instant(rs, "next_retry_at"),
        rs.getTimestamp("created_at").toInstant(),
        instant(rs, "started_at"),
        instant(rs, "completed_at"),
        true,
        rs.getString("fts_status"),
        rs.getString("embedding_status"));
  }

  private Instant instant(ResultSet rs, String name) throws SQLException {
    var value = rs.getTimestamp(name);
    return value == null ? null : value.toInstant();
  }
}
