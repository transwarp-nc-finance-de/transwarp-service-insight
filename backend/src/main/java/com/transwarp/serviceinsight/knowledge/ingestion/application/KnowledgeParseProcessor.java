package com.transwarp.serviceinsight.knowledge.ingestion.application;

import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeDocumentParser;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeDocumentParser.ParseFailure;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.ParseAttemptPolicy;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.ParseAttemptPolicy.Action;
import com.transwarp.serviceinsight.knowledge.ingestion.port.KnowledgeIngestionRepository;
import com.transwarp.serviceinsight.knowledge.ingestion.port.OriginalFileStorage;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeParseProcessor {
  private final KnowledgeIngestionRepository repository;
  private final OriginalFileStorage storage;
  private final KnowledgeDocumentParser parser;
  private final Clock clock;
  private final ParseAttemptPolicy attemptPolicy;
  private final Executor executor;

  public KnowledgeParseProcessor(
      KnowledgeIngestionRepository repository,
      OriginalFileStorage storage,
      KnowledgeDocumentParser parser,
      ParseAttemptPolicy attemptPolicy,
      Clock clock,
      @Qualifier("applicationTaskExecutor") Executor executor) {
    this.repository = repository;
    this.storage = storage;
    this.parser = parser;
    this.attemptPolicy = attemptPolicy;
    this.clock = clock;
    this.executor = executor;
  }

  public void enqueue(UUID taskId) {
    executor.execute(() -> process(taskId));
  }

  private void process(UUID taskId) {
    var input = repository.claimPendingTask(taskId, clock.instant());
    if (input.isEmpty()) return;
    try {
      var bytes = storage.read(input.get().storageKey());
      repository.completeTask(
          input.get(), parser.parse(bytes, input.get().mediaType()), clock.instant());
    } catch (ParseFailure failure) {
      handleFailure(input.get(), failure.code(), failure.getMessage(), failure.retryable());
    } catch (Exception failure) {
      handleFailure(
          input.get(),
          "PARSER_TEMPORARILY_UNAVAILABLE",
          "Parsing is temporarily unavailable.",
          true);
    }
  }

  private void handleFailure(
      com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.TaskInput
          input,
      String code,
      String message,
      boolean retryable) {
    if (attemptPolicy.action(retryable, input.attempt(), input.maxAttempts()) == Action.RETRY) {
      var nextRetryAt = clock.instant().plusMillis(100);
      repository.scheduleRetry(input, code, message, nextRetryAt);
      CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS, executor)
          .execute(() -> process(input.taskId()));
      return;
    }
    repository.failTask(input, code, message, false, clock.instant());
  }
}
