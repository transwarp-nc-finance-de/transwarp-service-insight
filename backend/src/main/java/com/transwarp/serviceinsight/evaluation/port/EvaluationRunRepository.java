package com.transwarp.serviceinsight.evaluation.port;

import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.EvaluationCaseResult;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.EvaluationRun;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.EvaluationSummary;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.IdempotencyRecord;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.StoredFailure;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationRunRepository {
  void lockIdempotency(String idempotencyKey);

  Optional<IdempotencyRecord> findIdempotency(String idempotencyKey);

  Optional<EvaluationRun> findById(java.util.UUID taskId);

  void create(
      EvaluationRun run,
      String note,
      String requestedBy,
      String idempotencyKey,
      String requestHash,
      Instant createdAt);

  Optional<EvaluationRun> claim(UUID taskId, Instant startedAt);

  void complete(
      UUID taskId,
      EvaluationSummary summary,
      List<EvaluationCaseResult> results,
      Instant completedAt);

  void fail(UUID taskId, String code, String message, Instant completedAt);

  List<EvaluationRun> findAll();

  List<StoredFailure> findFailures(UUID taskId);

  List<UUID> recoverIncomplete();
}
