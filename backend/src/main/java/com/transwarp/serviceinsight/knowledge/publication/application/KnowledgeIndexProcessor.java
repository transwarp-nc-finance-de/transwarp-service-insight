package com.transwarp.serviceinsight.knowledge.publication.application;

import com.transwarp.serviceinsight.knowledge.publication.domain.IndexAttemptPolicy;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingException;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingPort;
import com.transwarp.serviceinsight.knowledge.publication.port.KnowledgePublicationRepository;
import com.transwarp.serviceinsight.knowledge.publication.port.KnowledgePublicationRepository.IndexInput;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeIndexProcessor {
  private final KnowledgePublicationRepository repository;
  private final EmbeddingPort embedding;
  private final IndexAttemptPolicy attemptPolicy;
  private final Clock clock;
  private final Executor executor;

  public KnowledgeIndexProcessor(
      KnowledgePublicationRepository repository,
      EmbeddingPort embedding,
      IndexAttemptPolicy attemptPolicy,
      Clock clock,
      @Qualifier("applicationTaskExecutor") Executor executor) {
    this.repository = repository;
    this.embedding = embedding;
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
        .forEach(
            task -> {
              var delay = Duration.between(recoveredAt, task.nextRetryAt());
              if (delay.isNegative() || delay.isZero()) enqueue(task.taskId());
              else
                CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS, executor)
                    .execute(() -> process(task.taskId()));
            });
  }

  private void process(UUID taskId) {
    var claimed = repository.claimPendingTask(taskId, clock.instant());
    if (claimed.isEmpty()) return;
    var input = claimed.get();
    List<float[]> vectors = null;
    EmbeddingException embeddingFailure = null;
    try {
      vectors =
          embedding.embedPassages(input.chunks().stream().map(chunk -> chunk.text()).toList());
    } catch (EmbeddingException failure) {
      embeddingFailure = failure;
    } catch (IllegalArgumentException failure) {
      embeddingFailure =
          new EmbeddingException("EMBEDDING_INVALID_RESPONSE", "Embedding 返回内容不符合固定契约。", false);
    } catch (Exception failure) {
      embeddingFailure =
          new EmbeddingException("EMBEDDING_INTERNAL_ERROR", "向量分支发生内部错误，请人工检查。", false);
    }
    try {
      repository.markFtsPrepared(input);
    } catch (TransientDataAccessException failure) {
      handleFailure(input, "FTS", "FTS_TEMPORARILY_UNAVAILABLE", "全文索引暂时不可用。", true);
      return;
    } catch (Exception failure) {
      handleFailure(input, "FTS", "FTS_INTERNAL_ERROR", "全文索引构建失败，请人工检查。", false);
      return;
    }
    if (embeddingFailure != null) {
      handleFailure(
          input,
          "EMBEDDING",
          embeddingFailure.code(),
          embeddingFailure.getMessage(),
          embeddingFailure.retryable());
      return;
    }
    try {
      repository.completeTask(input, vectors, clock.instant());
    } catch (TransientDataAccessException failure) {
      handleFailure(input, "INDEX", "INDEX_TEMPORARILY_UNAVAILABLE", "原子发布暂时不可用。", true);
    } catch (Exception failure) {
      handleFailure(input, "INDEX", "INDEX_INTERNAL_ERROR", "原子发布失败，请人工检查。", false);
    }
  }

  private void handleFailure(
      IndexInput input, String failedBranch, String code, String safeMessage, boolean retryable) {
    if (attemptPolicy.shouldRetry(retryable, input.attempt(), input.maxAttempts())) {
      var delay = attemptPolicy.retryDelay(input.attempt());
      var failedAt = clock.instant();
      var nextRetryAt = failedAt.plus(delay);
      repository.scheduleRetry(input, failedBranch, code, safeMessage, failedAt, nextRetryAt);
      CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS, executor)
          .execute(() -> process(input.taskId()));
      return;
    }
    repository.failTask(input, failedBranch, code, safeMessage, false, clock.instant());
  }
}
