package com.transwarp.serviceinsight.audit.v2.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StructuredAuditModels {
  private StructuredAuditModels() {}

  public record StructuredAuditEvent(
      UUID eventId,
      String actorUserCode,
      String action,
      String subjectType,
      UUID subjectId,
      String outcome,
      Map<String, Object> metadata,
      Instant occurredAt,
      boolean mockData) {
    public StructuredAuditEvent {
      metadata = Map.copyOf(metadata);
    }
  }

  public record StoredAuditEvent(
      StructuredAuditEvent event, String productLineCode, String errorCode) {}

  public record PageMetadata(
      int page, int size, long totalItems, int totalPages, String sortBy, String sortDirection) {}

  public record AuditEventPage(List<StructuredAuditEvent> items, PageMetadata page) {}
}
