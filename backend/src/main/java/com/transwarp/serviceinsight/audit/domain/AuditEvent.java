package com.transwarp.serviceinsight.audit.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
    UUID id,
    BusinessEventType type,
    UUID precheckId,
    String subjectId,
    String policyVersion,
    Instant occurredAt,
    Map<String, String> metadata) {
  public AuditEvent {
    metadata = Map.copyOf(metadata);
  }
}
