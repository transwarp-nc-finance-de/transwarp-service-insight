package com.transwarp.serviceinsight.admin.reset.application;

import com.transwarp.serviceinsight.admin.reset.port.AdminResetRepository;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StoredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StructuredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.port.StructuredAuditPort;
import com.transwarp.serviceinsight.knowledge.ingestion.port.OriginalFileStorage;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class AdminResetProcessor {
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(AdminResetProcessor.class);

  private final AdminResetRepository resets;
  private final LocalMockDataResetService resetService;
  private final OriginalFileStorage files;
  private final StructuredAuditPort audit;
  private final Clock clock;
  private final Executor executor;

  public AdminResetProcessor(
      AdminResetRepository resets,
      LocalMockDataResetService resetService,
      OriginalFileStorage files,
      StructuredAuditPort audit,
      Clock clock,
      @Qualifier("applicationTaskExecutor") Executor executor) {
    this.resets = resets;
    this.resetService = resetService;
    this.files = files;
    this.audit = audit;
    this.clock = clock;
    this.executor = executor;
  }

  public void enqueue(UUID taskId) {
    executor.execute(() -> process(taskId));
  }

  @EventListener(ApplicationReadyEvent.class)
  public void recoverIncompleteResets() {
    resets.recoverIncomplete().forEach(this::enqueue);
  }

  void process(UUID taskId) {
    var claimed = resets.claim(taskId, clock.instant());
    if (claimed.isEmpty()) return;
    var reset = claimed.get();
    try {
      files.clearAll();
      resetService.rebuildDatabase();
      var completedAt = clock.instant();
      resets.complete(taskId, completedAt);
      audit.record(
          new StoredAuditEvent(
              new StructuredAuditEvent(
                  reset.auditEventId(),
                  reset.confirmedBy(),
                  "ADMIN_RESET_COMPLETED",
                  "AdminReset",
                  taskId,
                  "SUCCEEDED",
                  Map.of(
                      "environmentCode", "LOCAL",
                      "seedVersion", "local-mock-data-v1",
                      "evaluationSetVersion", "mock-eval-v1",
                      "mockData", true),
                  completedAt,
                  true),
              null,
              null));
    } catch (Exception failure) {
      LOG.error("Admin reset {} failed", taskId, failure);
      var completedAt = clock.instant();
      resets.fail(taskId, "RESET_FAILED", "本地模拟数据重置失败，请人工检查本地数据库与文件存储", completedAt);
      audit.record(
          new StoredAuditEvent(
              new StructuredAuditEvent(
                  reset.auditEventId(),
                  reset.confirmedBy(),
                  "ADMIN_RESET_FAILED",
                  "AdminReset",
                  taskId,
                  "FAILED",
                  Map.of("environmentCode", "LOCAL", "mockData", true),
                  completedAt,
                  true),
              null,
              "RESET_FAILED"));
    }
  }
}
