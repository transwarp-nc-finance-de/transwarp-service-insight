package com.transwarp.serviceinsight.admin.reset.application;

import com.transwarp.serviceinsight.admin.reset.domain.AdminResetModels.AdminReset;
import com.transwarp.serviceinsight.admin.reset.port.AdminResetRepository;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StoredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StructuredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.port.StructuredAuditPort;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminResetFinalizer {
  private final AdminResetRepository resets;
  private final StructuredAuditPort audit;

  public AdminResetFinalizer(AdminResetRepository resets, StructuredAuditPort audit) {
    this.resets = resets;
    this.audit = audit;
  }

  @Transactional
  public void succeed(AdminReset reset, Instant completedAt) {
    resets.complete(reset.taskId(), completedAt);
    audit.record(
        event(
            reset,
            "ADMIN_RESET_COMPLETED",
            "SUCCEEDED",
            Map.of(
                "environmentCode", "LOCAL",
                "seedVersion", "local-mock-data-v1",
                "evaluationSetVersion", "mock-eval-v1",
                "mockData", true),
            completedAt,
            null));
  }

  @Transactional
  public void fail(AdminReset reset, String code, String message, Instant completedAt) {
    resets.fail(reset.taskId(), code, message, completedAt);
    audit.record(
        event(
            reset,
            "ADMIN_RESET_FAILED",
            "FAILED",
            Map.of("environmentCode", "LOCAL", "mockData", true),
            completedAt,
            code));
  }

  private StoredAuditEvent event(
      AdminReset reset,
      String action,
      String outcome,
      Map<String, Object> metadata,
      Instant occurredAt,
      String errorCode) {
    return new StoredAuditEvent(
        new StructuredAuditEvent(
            reset.auditEventId(),
            reset.confirmedBy(),
            action,
            "AdminReset",
            reset.taskId(),
            outcome,
            metadata,
            occurredAt,
            true),
        null,
        errorCode);
  }
}
