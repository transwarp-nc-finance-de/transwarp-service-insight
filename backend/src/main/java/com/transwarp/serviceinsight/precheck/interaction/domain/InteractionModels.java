package com.transwarp.serviceinsight.precheck.interaction.domain;

import java.time.Instant;
import java.util.UUID;

public final class InteractionModels {
  private InteractionModels() {}

  public enum AdoptionStatus {
    ADOPTED,
    PARTIALLY_ADOPTED,
    IGNORED
  }

  public enum Helpfulness {
    HELPFUL,
    NOT_HELPFUL
  }

  public record Feedback(
      UUID feedbackId,
      UUID sessionId,
      UUID runId,
      String adoptionStatus,
      String helpfulness,
      String reason,
      Instant recordedAt,
      boolean mockData) {}

  public record FeedbackRecord(Feedback feedback, String ownerUserCode, String productLineCode) {}

  public record FeedbackCreationResult(Feedback feedback, boolean replayed) {}

  public record FeedbackIdempotency(String requestHash, FeedbackRecord record) {}

  public record SubmissionContinuation(
      UUID continuationId,
      UUID sessionId,
      String confirmedBy,
      Instant confirmedAt,
      String reason,
      UUID auditEventId,
      boolean mockData) {}

  public record ContinuationRecord(SubmissionContinuation continuation, String productLineCode) {}

  public record ContinuationCreationResult(SubmissionContinuation continuation, boolean replayed) {}

  public record ContinuationIdempotency(String requestHash, ContinuationRecord record) {}
}
