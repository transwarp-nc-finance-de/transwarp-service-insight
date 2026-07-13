package com.transwarp.serviceinsight.precheck.domain;

import java.util.UUID;

public record PrecheckRun(
    UUID id, UUID sessionId, int sequence, PrecheckStatus status, String inputSummary) {
  public PrecheckRun {
    if (id == null || sessionId == null) throw new IllegalArgumentException("运行标识不能为空");
    if (sequence < 1) throw new IllegalArgumentException("sequence 必须大于 0");
    if (status == null) throw new IllegalArgumentException("status 不能为空");
    inputSummary = inputSummary == null ? "" : inputSummary;
  }

  public PrecheckRun(UUID id, UUID sessionId, int sequence, PrecheckStatus status) {
    this(id, sessionId, sequence, status, "");
  }

  public PrecheckRun complete(PrecheckStatus completedStatus) {
    return new PrecheckRun(id, sessionId, sequence, completedStatus, inputSummary);
  }
}
