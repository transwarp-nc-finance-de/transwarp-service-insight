package com.transwarp.serviceinsight.precheck.retrieval.domain;

import com.transwarp.serviceinsight.precheck.retrieval.domain.HybridRetrievalPolicy.FusionResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RetrievalModels {
  private RetrievalModels() {}

  public record Capability(boolean available, String code, String message) {}

  public record RetrievalOutcome(
      String mode,
      Capability fts,
      Capability embedding,
      FusionResult fusion,
      List<EvidenceSnapshot> evidence) {
    public boolean degraded() {
      return !"HYBRID".equals(mode);
    }
  }

  public record EvidenceSnapshot(
      UUID evidenceId,
      UUID documentId,
      UUID versionId,
      UUID chunkId,
      String productLineCode,
      String title,
      String excerpt,
      String contentHash,
      String retrievalMode,
      Integer ftsRank,
      Integer vectorRank,
      double rrfScore,
      int selectedRank) {}

  public record EvidenceView(
      UUID evidenceId,
      EvidenceDocument document,
      UUID versionId,
      UUID chunkId,
      String excerpt,
      String contentHash,
      boolean mockData) {}

  public record EvidenceDocument(UUID documentId, String title) {}

  public record StoredEvidence(
      EvidenceSnapshot snapshot, UUID runId, Instant createdAt, String ownerUserCode) {}
}
