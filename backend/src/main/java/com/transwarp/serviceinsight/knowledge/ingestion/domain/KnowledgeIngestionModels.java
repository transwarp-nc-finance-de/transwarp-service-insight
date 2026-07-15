package com.transwarp.serviceinsight.knowledge.ingestion.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class KnowledgeIngestionModels {
  private KnowledgeIngestionModels() {}

  public record CatalogValue(String code, String displayName) {}

  public record Document(
      UUID documentId,
      String title,
      CatalogValue productLine,
      UUID currentPublishedVersionId,
      Instant createdAt,
      boolean mockData) {}

  public record Version(
      UUID versionId,
      UUID documentId,
      int revisionNumber,
      String status,
      String submittedBy,
      String approvedBy,
      Instant createdAt,
      Instant updatedAt,
      boolean mockData) {}

  public record TaskError(String code, String message, boolean retryable) {}

  public record ParseTask(
      UUID taskId,
      UUID resourceId,
      String status,
      int attempt,
      int maxAttempts,
      TaskError error,
      Instant nextRetryAt,
      Instant createdAt,
      Instant startedAt,
      Instant completedAt,
      boolean mockData) {}

  public record Created(Document document, Version version, ParseTask parseTask) {}

  public record IdempotencyRecord(String requestHash, Created created) {}

  public record UploadAggregate(
      UUID documentId,
      UUID versionId,
      UUID revisionId,
      UUID taskId,
      UUID fileId,
      String title,
      CatalogValue productLine,
      String sourceType,
      String createdBy,
      String storageKey,
      String contentHash,
      long sizeBytes,
      String mediaType,
      String originalName,
      String idempotencyKey,
      String requestHash,
      Instant createdAt) {}

  public record TaskInput(
      UUID taskId,
      UUID versionId,
      UUID revisionId,
      String storageKey,
      String mediaType,
      String cleanedText,
      int attempt,
      int maxAttempts) {}

  public record RecoveryTask(UUID taskId, Instant nextRetryAt) {}

  public record ParsedBlock(
      UUID blockId, int sequence, String structurePath, String text, String contentHash) {}

  public record Chunk(
      UUID chunkId,
      int sequence,
      String structurePath,
      String text,
      int tokenCount,
      String contentHash,
      String chunkingRuleVersion) {}

  public record ParseWarning(
      String code, String message, String structurePath, int occurrenceCount) {}

  public record ParsedDocument(
      String parserVersion,
      String parseResultHash,
      String chunkingRuleVersion,
      List<ParsedBlock> blocks,
      List<Chunk> chunks,
      List<ParseWarning> warnings) {}

  public record ChunkingRule(String version, String strategy, int maxTokens, int overlapTokens) {}

  public record ParsePreview(
      UUID versionId,
      UUID parseTaskId,
      String parseStatus,
      String parserVersion,
      String parseResultHash,
      long parsedBlockCount,
      long chunkCount,
      List<ParseWarning> warnings,
      ChunkingRule chunkingRule,
      boolean mockData) {}

  public record PageMetadata(
      int page, int size, long totalItems, int totalPages, String sortBy, String sortDirection) {}

  public record ParsedBlockPage(List<ParsedBlock> items, PageMetadata page) {}

  public record ChunkPage(List<Chunk> items, PageMetadata page) {}
}
