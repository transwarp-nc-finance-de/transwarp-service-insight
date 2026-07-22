package com.transwarp.serviceinsight.admin.reset.application;

import com.transwarp.serviceinsight.admin.reset.port.AdminResetRepository;
import com.transwarp.serviceinsight.admin.reset.port.LocalResetFileStore;
import java.time.Clock;
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
  private final LocalResetFileStore files;
  private final AdminResetFinalizer finalizer;
  private final Clock clock;
  private final Executor executor;

  public AdminResetProcessor(
      AdminResetRepository resets,
      LocalMockDataResetService resetService,
      LocalResetFileStore files,
      AdminResetFinalizer finalizer,
      Clock clock,
      @Qualifier("applicationTaskExecutor") Executor executor) {
    this.resets = resets;
    this.resetService = resetService;
    this.files = files;
    this.finalizer = finalizer;
    this.clock = clock;
    this.executor = executor;
  }

  public void enqueue(UUID taskId) {
    executor.execute(() -> process(taskId));
  }

  @EventListener(ApplicationReadyEvent.class)
  public void recoverIncompleteResets() {
    resets
        .findExhaustedRunning()
        .forEach(
            reset ->
                finalizer.fail(
                    reset, "RESET_ATTEMPTS_EXHAUSTED", "本地模拟数据重置已耗尽三次尝试", clock.instant()));
    resets.recoverIncomplete().forEach(this::enqueue);
  }

  synchronized void process(UUID taskId) {
    var claimed = resets.claim(taskId, clock.instant());
    if (claimed.isEmpty()) return;
    var reset = claimed.get();
    try {
      files.clearOriginalFiles();
      resetService.rebuildDatabase();
      var completedAt = clock.instant();
      finalizer.succeed(reset, completedAt);
    } catch (Exception failure) {
      LOG.error("Admin reset {} failed", taskId, failure);
      var completedAt = clock.instant();
      finalizer.fail(reset, "RESET_FAILED", "本地模拟数据重置失败，请人工检查本地数据库与文件存储", completedAt);
    }
  }
}
