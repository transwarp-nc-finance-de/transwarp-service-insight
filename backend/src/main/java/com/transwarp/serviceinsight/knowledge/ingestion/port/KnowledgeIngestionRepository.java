package com.transwarp.serviceinsight.knowledge.ingestion.port;

import com.transwarp.serviceinsight.identity.domain.IdentityContext;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ChunkPage;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.Created;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.IdempotencyRecord;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParsePreview;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParseTask;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParsedBlockPage;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParsedDocument;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.TaskInput;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.UploadAggregate;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeIngestionRepository {
  Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

  Created create(UploadAggregate aggregate);

  Optional<ParseTask> findVisibleTask(UUID taskId, IdentityContext identity);

  Optional<ParseTask> findVisibleTaskByVersion(UUID versionId, IdentityContext identity);

  Optional<ParsePreview> findPreview(UUID versionId, IdentityContext identity);

  ParsedBlockPage findBlocks(UUID versionId, int page, int size);

  ChunkPage findChunks(UUID versionId, int page, int size);

  Optional<TaskInput> claimPendingTask(UUID taskId, Instant startedAt);

  void completeTask(TaskInput input, ParsedDocument document, Instant completedAt);

  void scheduleRetry(TaskInput input, String errorCode, String safeMessage, Instant nextRetryAt);

  void failTask(
      TaskInput input,
      String errorCode,
      String safeMessage,
      boolean retryable,
      Instant completedAt);
}
