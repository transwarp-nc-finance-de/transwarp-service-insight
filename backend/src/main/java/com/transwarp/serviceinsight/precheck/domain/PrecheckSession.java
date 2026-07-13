package com.transwarp.serviceinsight.precheck.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PrecheckSession {
  private final UUID id;
  private final UUID precheckId;
  private final String hostRequestId;
  private final List<PrecheckRun> runs = new ArrayList<>();
  private PrecheckStatus status = PrecheckStatus.RECEIVED;

  public PrecheckSession(UUID id, UUID precheckId, String hostRequestId) {
    if (id == null || precheckId == null) throw new IllegalArgumentException("会话标识不能为空");
    this.id = id;
    this.precheckId = precheckId;
    this.hostRequestId = hostRequestId;
  }

  /**
   * @deprecated compatibility constructor for the pre-workflow domain tests.
   */
  @Deprecated
  public PrecheckSession(UUID id, PrecheckStatus status) {
    this(id, UUID.randomUUID(), null);
    this.status = status;
  }

  public synchronized PrecheckRun startRun(String inputSummary) {
    var run =
        new PrecheckRun(
            UUID.randomUUID(), id, runs.size() + 1, PrecheckStatus.RECEIVED, inputSummary);
    runs.add(run);
    status = PrecheckStatus.VALIDATED;
    return run;
  }

  public synchronized void completeRun(UUID runId, PrecheckStatus completedStatus) {
    for (var index = 0; index < runs.size(); index++) {
      if (runs.get(index).id().equals(runId)) {
        runs.set(index, runs.get(index).complete(completedStatus));
        status = completedStatus;
        return;
      }
    }
    throw new IllegalArgumentException("运行不存在");
  }

  public UUID id() {
    return id;
  }

  public UUID precheckId() {
    return precheckId;
  }

  public String hostRequestId() {
    return hostRequestId;
  }

  public synchronized PrecheckStatus status() {
    return status;
  }

  public synchronized List<PrecheckRun> runs() {
    return List.copyOf(runs);
  }
}
