package com.transwarp.serviceinsight.knowledge.publication.port;

import com.transwarp.serviceinsight.knowledge.publication.domain.IndexTask;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgePublicationRepository {
  void lockIdempotency(String commandType, String idempotencyKey);

  Optional<IdempotencyRecord> findIdempotency(String commandType, String idempotencyKey);

  Optional<ApprovedVersion> lockApprovedVersion(UUID versionId);

  boolean hasActiveTask(UUID versionId);

  IndexTask createTask(
      UUID taskId,
      ApprovedVersion version,
      String actor,
      String idempotencyKey,
      String requestHash,
      Instant createdAt);

  Optional<IndexInput> claimPendingTask(UUID taskId, Instant startedAt);

  void markFtsPrepared(IndexInput input);

  void completeTask(IndexInput input, List<float[]> embeddings, Instant completedAt);

  void scheduleRetry(
      IndexInput input,
      String failedBranch,
      String errorCode,
      String safeMessage,
      Instant failedAt,
      Instant nextRetryAt);

  void failTask(
      IndexInput input,
      String failedBranch,
      String errorCode,
      String safeMessage,
      boolean retryable,
      Instant completedAt);

  List<RecoveryTask> recoverIncompleteTasks(Instant recoveredAt);

  Optional<IndexTask> findVisibleTask(UUID taskId, String userCode, List<String> productLines);

  TaskPage listVisibleTasks(
      List<String> productLines,
      int page,
      int size,
      String sortDirection,
      String sortBy,
      String status,
      UUID versionId);

  record IdempotencyRecord(String requestHash, IndexTask task) {}

  record ApprovedVersion(UUID versionId, UUID revisionId, String productLineCode) {}

  record IndexChunk(UUID chunkId, String text, String contentHash) {}

  record IndexInput(
      UUID taskId,
      UUID versionId,
      UUID revisionId,
      UUID documentId,
      String productLineCode,
      String actorUserCode,
      int attempt,
      int maxAttempts,
      List<IndexChunk> chunks) {}

  record RecoveryTask(UUID taskId, Instant nextRetryAt) {}

  record PageMetadata(
      int page, int size, long totalItems, int totalPages, String sortBy, String sortDirection) {}

  record TaskPage(List<IndexTask> items, PageMetadata page) {}
}
