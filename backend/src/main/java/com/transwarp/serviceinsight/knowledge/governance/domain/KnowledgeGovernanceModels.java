package com.transwarp.serviceinsight.knowledge.governance.domain;

import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.CatalogValue;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.ParseTask;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class KnowledgeGovernanceModels {
  private KnowledgeGovernanceModels() {}

  public record Version(
      UUID versionId,
      UUID documentId,
      int revisionNumber,
      String status,
      String submittedBy,
      String approvedBy,
      Instant createdAt,
      Instant updatedAt,
      boolean mockData) {}

  public record VersionState(
      Version version,
      String createdBy,
      String productLineCode,
      UUID currentDraftRevisionId,
      int currentDraftRevisionNumber,
      String parseStatus,
      String parseResultHash,
      Set<String> warningCodes) {}

  public record DraftRevision(
      UUID revisionId,
      UUID versionId,
      int revisionNumber,
      String title,
      CatalogValue productLine,
      String cleanedTextHash,
      List<String> parseWarningNotes,
      Instant createdAt,
      boolean mockData) {}

  public record RevisionCreated(DraftRevision revision, ParseTask parseTask) {}

  public record CommandResult(Version version, UUID auditEventId) {}

  public record IdempotencyRecord(
      String requestHash, RevisionCreated revisionResult, CommandResult commandResult) {}

  public record NewRevision(
      UUID revisionId,
      UUID taskId,
      UUID versionId,
      int revisionNumber,
      String title,
      CatalogValue productLine,
      String cleanedText,
      String cleanedTextHash,
      List<String> parseWarningNotes,
      String actor,
      String idempotencyKey,
      String requestHash,
      Instant occurredAt) {}

  public record GovernanceCommand(
      String commandType,
      UUID versionId,
      UUID revisionId,
      String actor,
      String targetStatus,
      String submittedBy,
      String approvedBy,
      String parseResultHash,
      Set<String> acknowledgedWarningCodes,
      String reason,
      String idempotencyKey,
      String requestHash,
      UUID auditEventId,
      Instant occurredAt) {}
}
