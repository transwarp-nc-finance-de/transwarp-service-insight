package com.transwarp.serviceinsight.knowledge.ingestion.application;

import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeDocumentParser;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeDocumentParser.ParseFailure;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.ParseAttemptPolicy;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.ParseAttemptPolicy.Action;
import com.transwarp.serviceinsight.knowledge.ingestion.port.KnowledgeIngestionRepository;
import com.transwarp.serviceinsight.knowledge.ingestion.port.OriginalFileStorage;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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

  @EventListener(ApplicationReadyEvent.class)
  public void recoverIncompleteTasks() {
    var recoveredAt = clock.instant();
    repository
        .recoverIncompleteTasks(recoveredAt)
        .forEach(task -> scheduleRecovered(task, recoveredAt));
  }

  private void scheduleRecovered(
      com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.RecoveryTask
          task,
      java.time.Instant recoveredAt) {
    var delay = Duration.between(recoveredAt, task.nextRetryAt());
    if (delay.isNegative() || delay.isZero()) {
      enqueue(task.taskId());
      return;
    }
    CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS, executor)
        .execute(() -> process(task.taskId()));
  }

  private void process(UUID taskId) {
    var input = repository.claimPendingTask(taskId, clock.instant());
    if (input.isEmpty()) return;
    try {
      var bytes = storage.read(input.get().storageKey());
      repository.completeTask(
          input.get(),
          parser.parse(bytes, input.get().mediaType(), input.get().taskId()),
          clock.instant());
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
      var delay = attemptPolicy.retryDelay(input.attempt());
      var nextRetryAt = clock.instant().plus(delay);
      repository.scheduleRetry(input, code, message, nextRetryAt);
      CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS, executor)
          .execute(() -> process(input.taskId()));
      return;
    }
    repository.failTask(input, code, message, false, clock.instant());
  }
}
