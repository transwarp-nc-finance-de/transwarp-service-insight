package com.transwarp.serviceinsight.knowledge.publication.domain;

import java.time.Instant;
import java.util.UUID;

public record IndexTask(
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
    boolean mockData,
    String ftsStatus,
    String embeddingStatus) {

  public record TaskError(String code, String message, boolean retryable) {}
}
