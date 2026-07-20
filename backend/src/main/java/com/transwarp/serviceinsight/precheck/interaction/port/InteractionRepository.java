package com.transwarp.serviceinsight.precheck.interaction.port;

import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.ContinuationIdempotency;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.ContinuationRecord;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.FeedbackIdempotency;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.FeedbackRecord;
import java.util.Optional;
import java.util.UUID;

public interface InteractionRepository {
  Optional<FeedbackIdempotency> findFeedbackIdempotency(String idempotencyKey);

  void saveFeedback(FeedbackRecord record, String idempotencyKey, String requestHash);

  Optional<FeedbackRecord> findFeedback(UUID feedbackId);

  Optional<ContinuationIdempotency> findContinuationIdempotency(String idempotencyKey);

  void saveContinuation(ContinuationRecord record, String idempotencyKey, String requestHash);

  Optional<ContinuationRecord> findContinuation(UUID continuationId);
}
