package com.transwarp.serviceinsight.knowledge.ingestion.infrastructure.jdbc;

import com.transwarp.serviceinsight.identity.domain.IdentityContext;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.CatalogValue;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.Chunk;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ChunkPage;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ChunkingRule;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.Created;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.Document;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.IdempotencyRecord;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.PageMetadata;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParsePreview;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParseTask;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParseWarning;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParsedBlock;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParsedBlockPage;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParsedDocument;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.TaskError;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.TaskInput;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.UploadAggregate;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.Version;
import com.transwarp.serviceinsight.knowledge.ingestion.port.KnowledgeIngestionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcKnowledgeIngestionRepository implements KnowledgeIngestionRepository {
  private final JdbcTemplate jdbc;

  public JdbcKnowledgeIngestionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey) {
    var records =
        jdbc.query(
            "SELECT request_hash, document_id, version_id, task_id FROM knowledge_ingestion_idempotency WHERE idempotency_key = ?",
            (resultSet, rowNumber) ->
                new IdempotencyRecord(
                    resultSet.getString("request_hash"),
                    loadCreated(
                        resultSet.getObject("document_id", java.util.UUID.class),
                        resultSet.getObject("version_id", java.util.UUID.class),
                        resultSet.getObject("task_id", java.util.UUID.class))),
            idempotencyKey);
    return records.stream().findFirst();
  }

  @Override
  @Transactional
  public Created create(UploadAggregate value) {
    jdbc.update(
        "INSERT INTO knowledge_original_file(file_id, storage_key, content_hash, size_bytes, media_type, original_name, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
        value.fileId(),
        value.storageKey(),
        value.contentHash(),
        value.sizeBytes(),
        value.mediaType(),
        value.originalName(),
        Timestamp.from(value.createdAt()));
    jdbc.update(
        "INSERT INTO knowledge_document(document_id, title, product_line_code, product_line_display_name, source_type, created_by, current_published_version_id, created_at, mock_data) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, TRUE)",
        value.documentId(),
        value.title(),
        value.productLine().code(),
        value.productLine().displayName(),
        value.sourceType(),
        value.createdBy(),
        Timestamp.from(value.createdAt()));
    jdbc.update(
        "INSERT INTO knowledge_version_v2(version_id, document_id, revision_number, status, created_by, submitted_by, approved_by, original_file_id, created_at, updated_at, mock_data) VALUES (?, ?, 1, 'DRAFT', ?, NULL, NULL, ?, ?, ?, TRUE)",
        value.versionId(),
        value.documentId(),
        value.createdBy(),
        value.fileId(),
        Timestamp.from(value.createdAt()),
        Timestamp.from(value.createdAt()));
    jdbc.update(
        "INSERT INTO knowledge_draft_revision(revision_id, version_id, revision_number, title, product_line_code, created_at) VALUES (?, ?, 1, ?, ?, ?)",
        value.revisionId(),
        value.versionId(),
        value.title(),
        value.productLine().code(),
        Timestamp.from(value.createdAt()));
    jdbc.update(
        "INSERT INTO parse_task(task_id, resource_id, status, attempt, max_attempts, created_at) VALUES (?, ?, 'PENDING', 0, 3, ?)",
        value.taskId(),
        value.versionId(),
        Timestamp.from(value.createdAt()));
    jdbc.update(
        "INSERT INTO knowledge_ingestion_idempotency(idempotency_key, request_hash, document_id, version_id, task_id, created_at) VALUES (?, ?, ?, ?, ?, ?)",
        value.idempotencyKey(),
        value.requestHash(),
        value.documentId(),
        value.versionId(),
        value.taskId(),
        Timestamp.from(value.createdAt()));
    return loadCreated(value.documentId(), value.versionId(), value.taskId());
  }

  @Override
  public Optional<ParseTask> findVisibleTask(UUID taskId, IdentityContext identity) {
    var tasks =
        jdbc.query(
            """
            SELECT t.*
            FROM parse_task t
            JOIN knowledge_version_v2 v ON v.version_id = t.resource_id
            JOIN knowledge_document d ON d.document_id = v.document_id
            WHERE t.task_id = ?
              AND d.product_line_code IN (%s)
              AND v.created_by = ?
            """
                .formatted(placeholders(identity.productLineCodes().size())),
            (resultSet, rowNumber) -> task(resultSet),
            parameters(taskId, identity));
    return tasks.stream().findFirst();
  }

  @Override
  public Optional<ParseTask> findVisibleTaskByVersion(UUID versionId, IdentityContext identity) {
    var tasks =
        jdbc.query(
            """
            SELECT t.* FROM parse_task t
            JOIN knowledge_version_v2 v ON v.version_id = t.resource_id
            JOIN knowledge_document d ON d.document_id = v.document_id
            WHERE v.version_id = ? AND d.product_line_code IN (%s)
              AND ((? AND v.created_by = ?) OR (? AND v.status IN ('IN_REVIEW', 'APPROVED', 'PUBLISHED', 'SUPERSEDED')))
            """
                .formatted(placeholders(identity.productLineCodes().size())),
            (resultSet, rowNumber) -> task(resultSet),
            previewParameters(versionId, identity));
    return tasks.stream().findFirst();
  }

  @Override
  public Optional<ParsePreview> findPreview(UUID versionId, IdentityContext identity) {
    var previews =
        jdbc.query(
            """
            SELECT r.*, t.status FROM knowledge_parse_result r
            JOIN parse_task t ON t.task_id = r.task_id
            JOIN knowledge_version_v2 v ON v.version_id = r.version_id
            JOIN knowledge_document d ON d.document_id = v.document_id
            WHERE r.version_id = ? AND d.product_line_code IN (%s)
              AND ((? AND v.created_by = ?) OR (? AND v.status IN ('IN_REVIEW', 'APPROVED', 'PUBLISHED', 'SUPERSEDED')))
            """
                .formatted(placeholders(identity.productLineCodes().size())),
            (resultSet, rowNumber) ->
                new ParsePreview(
                    resultSet.getObject("version_id", UUID.class),
                    resultSet.getObject("task_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getString("parser_version"),
                    resultSet.getString("parse_result_hash"),
                    resultSet.getLong("parsed_block_count"),
                    resultSet.getLong("chunk_count"),
                    warnings(versionId),
                    new ChunkingRule(
                        resultSet.getString("chunking_rule_version"), "STRUCTURE_FIRST", 400, 50),
                    true),
            previewParameters(versionId, identity));
    return previews.stream().findFirst();
  }

  @Override
  public ParsedBlockPage findBlocks(UUID versionId, int page, int size) {
    var total =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM knowledge_parsed_block WHERE version_id = ?",
            Long.class,
            versionId);
    var items =
        jdbc.query(
            "SELECT * FROM knowledge_parsed_block WHERE version_id = ? ORDER BY sequence ASC LIMIT ? OFFSET ?",
            (resultSet, rowNumber) ->
                new ParsedBlock(
                    resultSet.getObject("block_id", UUID.class),
                    resultSet.getInt("sequence"),
                    resultSet.getString("structure_path"),
                    resultSet.getString("text_content"),
                    resultSet.getString("content_hash")),
            versionId,
            size,
            (page - 1) * size);
    return new ParsedBlockPage(items, page(page, size, total));
  }

  @Override
  public ChunkPage findChunks(UUID versionId, int page, int size) {
    var total =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM knowledge_chunk WHERE version_id = ?", Long.class, versionId);
    var items =
        jdbc.query(
            "SELECT * FROM knowledge_chunk WHERE version_id = ? ORDER BY sequence ASC LIMIT ? OFFSET ?",
            (resultSet, rowNumber) ->
                new Chunk(
                    resultSet.getObject("chunk_id", UUID.class),
                    resultSet.getInt("sequence"),
                    resultSet.getString("structure_path"),
                    resultSet.getString("text_content"),
                    resultSet.getInt("token_count"),
                    resultSet.getString("content_hash"),
                    resultSet.getString("chunking_rule_version")),
            versionId,
            size,
            (page - 1) * size);
    return new ChunkPage(items, page(page, size, total));
  }

  @Override
  @Transactional
  public Optional<TaskInput> claimPendingTask(UUID taskId, Instant startedAt) {
    var updated =
        jdbc.update(
            "UPDATE parse_task SET status = 'RUNNING', attempt = attempt + 1, started_at = ?, next_retry_at = NULL WHERE task_id = ? AND status = 'PENDING' AND attempt < max_attempts",
            Timestamp.from(startedAt),
            taskId);
    if (updated == 0) return Optional.empty();
    return jdbc
        .query(
            """
            SELECT t.task_id, t.resource_id, t.attempt, t.max_attempts, f.storage_key, f.media_type
            FROM parse_task t
            JOIN knowledge_version_v2 v ON v.version_id = t.resource_id
            JOIN knowledge_original_file f ON f.file_id = v.original_file_id
            WHERE t.task_id = ?
            """,
            (resultSet, rowNumber) ->
                new TaskInput(
                    resultSet.getObject("task_id", UUID.class),
                    resultSet.getObject("resource_id", UUID.class),
                    resultSet.getString("storage_key"),
                    resultSet.getString("media_type"),
                    resultSet.getInt("attempt"),
                    resultSet.getInt("max_attempts")),
            taskId)
        .stream()
        .findFirst();
  }

  @Override
  @Transactional
  public void completeTask(TaskInput input, ParsedDocument document, Instant completedAt) {
    jdbc.update(
        "INSERT INTO knowledge_parse_result(version_id, task_id, parser_version, parse_result_hash, parsed_block_count, chunk_count, chunking_rule_version) VALUES (?, ?, ?, ?, ?, ?, ?)",
        input.versionId(),
        input.taskId(),
        document.parserVersion(),
        document.parseResultHash(),
        document.blocks().size(),
        document.chunks().size(),
        document.chunkingRuleVersion());
    for (var block : document.blocks()) {
      jdbc.update(
          "INSERT INTO knowledge_parsed_block(block_id, version_id, sequence, structure_path, text_content, content_hash) VALUES (?, ?, ?, ?, ?, ?)",
          block.blockId(),
          input.versionId(),
          block.sequence(),
          block.structurePath(),
          block.text(),
          block.contentHash());
    }
    for (var chunk : document.chunks()) {
      jdbc.update(
          "INSERT INTO knowledge_chunk(chunk_id, version_id, sequence, structure_path, text_content, token_count, content_hash, chunking_rule_version) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
          chunk.chunkId(),
          input.versionId(),
          chunk.sequence(),
          chunk.structurePath(),
          chunk.text(),
          chunk.tokenCount(),
          chunk.contentHash(),
          chunk.chunkingRuleVersion());
    }
    for (var warning : document.warnings()) {
      jdbc.update(
          "INSERT INTO knowledge_parse_warning(warning_id, version_id, warning_code, message, structure_path, occurrence_count) VALUES (?, ?, ?, ?, ?, ?)",
          UUID.randomUUID(),
          input.versionId(),
          warning.code(),
          warning.message(),
          warning.structurePath(),
          warning.occurrenceCount());
    }
    jdbc.update(
        "UPDATE parse_task SET status = 'SUCCEEDED', completed_at = ?, error_code = NULL, error_message = NULL, error_retryable = NULL WHERE task_id = ? AND status = 'RUNNING'",
        Timestamp.from(completedAt),
        input.taskId());
  }

  @Override
  public void failTask(
      TaskInput input,
      String errorCode,
      String safeMessage,
      boolean retryable,
      Instant completedAt) {
    jdbc.update(
        "UPDATE parse_task SET status = 'FAILED', error_code = ?, error_message = ?, error_retryable = ?, completed_at = ? WHERE task_id = ? AND status = 'RUNNING'",
        errorCode,
        safeMessage,
        retryable,
        Timestamp.from(completedAt),
        input.taskId());
  }

  @Override
  public void scheduleRetry(
      TaskInput input, String errorCode, String safeMessage, Instant nextRetryAt) {
    jdbc.update(
        "UPDATE parse_task SET status = 'PENDING', error_code = ?, error_message = ?, error_retryable = TRUE, next_retry_at = ? WHERE task_id = ? AND status = 'RUNNING'",
        errorCode,
        safeMessage,
        Timestamp.from(nextRetryAt),
        input.taskId());
  }

  private Created loadCreated(
      java.util.UUID documentId, java.util.UUID versionId, java.util.UUID taskId) {
    var document =
        jdbc.queryForObject(
            "SELECT * FROM knowledge_document WHERE document_id = ?",
            (resultSet, rowNumber) -> document(resultSet),
            documentId);
    var version =
        jdbc.queryForObject(
            "SELECT * FROM knowledge_version_v2 WHERE version_id = ?",
            (resultSet, rowNumber) -> version(resultSet),
            versionId);
    var task =
        jdbc.queryForObject(
            "SELECT * FROM parse_task WHERE task_id = ?",
            (resultSet, rowNumber) -> task(resultSet),
            taskId);
    return new Created(document, version, task);
  }

  private Document document(ResultSet resultSet) throws SQLException {
    return new Document(
        resultSet.getObject("document_id", java.util.UUID.class),
        resultSet.getString("title"),
        new CatalogValue(
            resultSet.getString("product_line_code"),
            resultSet.getString("product_line_display_name")),
        resultSet.getObject("current_published_version_id", java.util.UUID.class),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getBoolean("mock_data"));
  }

  private Version version(ResultSet resultSet) throws SQLException {
    return new Version(
        resultSet.getObject("version_id", java.util.UUID.class),
        resultSet.getObject("document_id", java.util.UUID.class),
        resultSet.getInt("revision_number"),
        resultSet.getString("status"),
        resultSet.getString("submitted_by"),
        resultSet.getString("approved_by"),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getTimestamp("updated_at").toInstant(),
        resultSet.getBoolean("mock_data"));
  }

  private ParseTask task(ResultSet resultSet) throws SQLException {
    var errorCode = resultSet.getString("error_code");
    return new ParseTask(
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
        true);
  }

  private String placeholders(int size) {
    return String.join(",", java.util.Collections.nCopies(size, "?"));
  }

  private Object[] parameters(UUID taskId, IdentityContext identity) {
    var parameters = new java.util.ArrayList<>();
    parameters.add(taskId);
    parameters.addAll(identity.productLineCodes());
    parameters.add(identity.userCode());
    return parameters.toArray();
  }

  private Object[] previewParameters(UUID versionId, IdentityContext identity) {
    var parameters = new java.util.ArrayList<>();
    parameters.add(versionId);
    parameters.addAll(identity.productLineCodes());
    parameters.add(identity.hasRole(Role.KNOWLEDGE_EDITOR));
    parameters.add(identity.userCode());
    parameters.add(identity.hasRole(Role.KNOWLEDGE_REVIEWER));
    return parameters.toArray();
  }

  private java.time.Instant instant(ResultSet resultSet, String column) throws SQLException {
    var timestamp = resultSet.getTimestamp(column);
    return timestamp == null ? null : timestamp.toInstant();
  }

  private java.util.List<ParseWarning> warnings(UUID versionId) {
    return jdbc.query(
        "SELECT * FROM knowledge_parse_warning WHERE version_id = ? ORDER BY warning_code, structure_path LIMIT 100",
        (resultSet, rowNumber) ->
            new ParseWarning(
                resultSet.getString("warning_code"),
                resultSet.getString("message"),
                resultSet.getString("structure_path"),
                resultSet.getInt("occurrence_count")),
        versionId);
  }

  private PageMetadata page(int page, int size, long total) {
    return new PageMetadata(
        page, size, total, (int) Math.ceil((double) total / size), "sequence", "ASC");
  }
}
