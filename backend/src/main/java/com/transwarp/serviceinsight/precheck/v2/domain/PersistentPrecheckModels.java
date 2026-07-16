package com.transwarp.serviceinsight.precheck.v2.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PersistentPrecheckModels {
  private PersistentPrecheckModels() {}

  public record CatalogValue(String code, String displayName) {}

  public record IssueTypeValue(String code, String displayName) {}

  public record AdditionalInformationItem(String fieldCode, String displayName, String value) {}

  public record AttachmentMetadata(
      String attachmentId, String fileName, String mediaType, long byteSize) {}

  public record PrecheckContext(
      String sourceSystem,
      String hostRequestId,
      String formSchemaVersion,
      IssueTypeValue issueType,
      CatalogValue productLine,
      CatalogValue product,
      CatalogValue component,
      String version,
      CatalogValue severity,
      CatalogValue serviceType,
      String title,
      String descriptionPlainText,
      List<AdditionalInformationItem> additionalInformation,
      String impactScope,
      List<AttachmentMetadata> attachments) {
    public PrecheckContext {
      additionalInformation =
          additionalInformation == null ? List.of() : List.copyOf(additionalInformation);
      attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
  }

  public record CapabilityStatus(boolean available, String code, String message) {}

  public record RetrievalDegradation(
      String mode, CapabilityStatus fts, CapabilityStatus embedding, boolean degraded) {}

  public record CompletenessAssessment(
      boolean complete,
      String policyVersion,
      List<String> missingFieldCodes,
      List<String> reasons) {}

  public record PrecheckResult(
      String summary,
      List<String> recommendations,
      CompletenessAssessment completeness,
      String confidence,
      List<String> confidenceReasons,
      List<Map<String, Object>> evidence,
      List<String> humanInterventionAdvice,
      List<String> missingInformation,
      List<String> allowedActions,
      String disclaimer,
      RetrievalDegradation retrieval,
      boolean mockData) {}

  public record PrecheckRun(
      UUID runId,
      UUID sessionId,
      int sequence,
      String status,
      PrecheckContext contextSnapshot,
      PrecheckResult result,
      Instant createdAt,
      Instant completedAt) {}

  public record PrecheckSession(
      UUID sessionId,
      String ownerUserCode,
      String status,
      String terminationReason,
      PrecheckRun latestRun,
      int runCount,
      int maxRuns,
      Instant createdAt,
      Instant updatedAt,
      boolean mockData) {}

  public record CompletenessPolicy(
      String policyVersion,
      IssueTypeValue issueType,
      List<String> generalFieldCodes,
      List<String> issueSpecificFieldCodes,
      Instant createdAt,
      boolean mockData) {}

  public record CreationResult(PrecheckSession session, boolean replayed) {}

  public record RunCreationResult(PrecheckRun run, boolean replayed) {}

  public record SessionTermination(
      UUID sessionId,
      String status,
      String reason,
      Instant terminatedAt,
      UUID auditEventId,
      boolean mockData) {}

  public record TerminationResult(SessionTermination termination, boolean replayed) {}

  public record PageMetadata(
      int page, int size, long totalItems, int totalPages, String sortBy, String sortDirection) {}

  public record PrecheckSessionPage(List<PrecheckSession> items, PageMetadata page) {}

  public record PrecheckRunPage(List<PrecheckRun> items, PageMetadata page) {}

  public record CompletenessPolicyPage(List<CompletenessPolicy> items, PageMetadata page) {}
}
