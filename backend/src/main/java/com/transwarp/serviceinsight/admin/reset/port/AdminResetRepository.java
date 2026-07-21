package com.transwarp.serviceinsight.admin.reset.port;

import com.transwarp.serviceinsight.admin.reset.domain.AdminResetModels.AdminReset;
import com.transwarp.serviceinsight.admin.reset.domain.AdminResetModels.IdempotencyRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminResetRepository {
  void lockIdempotency(String idempotencyKey);

  Optional<IdempotencyRecord> findIdempotency(String idempotencyKey);

  Optional<AdminReset> findById(UUID taskId);

  List<AdminReset> findAll();

  void create(
      AdminReset reset,
      String reason,
      String idempotencyKey,
      String requestHash,
      Instant createdAt);

  Optional<AdminReset> claim(UUID taskId, Instant startedAt);

  void complete(UUID taskId, Instant completedAt);

  void fail(UUID taskId, String code, String message, Instant completedAt);

  List<UUID> recoverIncomplete();
}
