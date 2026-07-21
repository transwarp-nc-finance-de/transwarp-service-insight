package com.transwarp.serviceinsight.admin.reset.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminResetModels {
  private AdminResetModels() {}

  public record TaskError(String code, String message, boolean retryable) {}

  public record AdminReset(
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
      String environmentCode,
      String confirmedBy,
      UUID auditEventId) {}

  public record CreationResult(AdminReset reset, boolean replayed) {}

  public record IdempotencyRecord(String requestHash, AdminReset reset) {}

  public record PageMetadata(
      int page, int size, long totalItems, int totalPages, String sortBy, String sortDirection) {}

  public record AdminResetPage(List<AdminReset> items, PageMetadata page) {}
}
