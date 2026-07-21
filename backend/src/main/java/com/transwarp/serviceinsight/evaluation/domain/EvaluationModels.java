package com.transwarp.serviceinsight.evaluation.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EvaluationModels {
  public static final String DISCLAIMER = "小样本工程评估，不代表生产效果";

  private EvaluationModels() {}

  public record TaskError(String code, String message, boolean retryable) {}

  public record EvaluationSummary(
      int sampleCount,
      boolean gatePassed,
      double permissionLeakageRate,
      double citationErrorRate,
      double degradationPassRate,
      double recallAt5,
      String disclaimer) {}

  public record EvaluationRun(
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
      String evaluationSetVersion,
      EvaluationSummary summary) {}

  public record CreationResult(EvaluationRun run, boolean replayed) {}

  public record IdempotencyRecord(String requestHash, EvaluationRun run) {}

  public record EvaluationCaseResult(
      String caseId,
      List<String> scenarioTags,
      List<String> failedChecks,
      List<String> failureCodes,
      String expectedSummary,
      String actualSummary,
      boolean passed,
      UUID traceSessionId,
      UUID traceRunId) {}

  public record PageMetadata(
      int page, int size, long totalItems, int totalPages, String sortBy, String sortDirection) {}

  public record EvaluationRunPage(List<EvaluationRun> items, PageMetadata page) {}

  public record StoredFailure(
      String caseId,
      List<String> scenarioTags,
      List<String> failedChecks,
      List<String> failureCodes,
      String expectedSummary,
      String actualSummary) {}

  public record EvaluationFailure(
      String caseId,
      List<String> scenarioTags,
      List<String> failedChecks,
      List<String> failureCodes,
      Map<String, Object> expected,
      Map<String, Object> actual,
      boolean mockData) {}

  public record EvaluationFailurePage(List<EvaluationFailure> items, PageMetadata page) {}

  public record Metrics(
      Instant from,
      Instant to,
      int precheckCount,
      double successRate,
      double degradationRate,
      double averageRunCount,
      double informationSupplementRate,
      double evidenceHitRate,
      double adoptionRate,
      double continuationRate,
      long retrievalP95Ms,
      long embeddingP95Ms,
      boolean mockData) {}
}
