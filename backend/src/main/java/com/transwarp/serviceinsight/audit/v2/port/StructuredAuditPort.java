package com.transwarp.serviceinsight.audit.v2.port;

import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StoredAuditEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StructuredAuditPort {
  void record(StoredAuditEvent event);

  List<StoredAuditEvent> find(
      List<String> productLineCodes,
      String action,
      UUID subjectId,
      int offset,
      int limit,
      String sortBy,
      String sortDirection);

  long count(List<String> productLineCodes, String action, UUID subjectId);

  Optional<StoredAuditEvent> findById(UUID eventId, List<String> productLineCodes);
}
