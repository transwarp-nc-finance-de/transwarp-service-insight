package com.transwarp.serviceinsight.precheck.domain;

import java.util.UUID;

public record PrecheckSession(UUID id, PrecheckStatus status) {
  public PrecheckSession {
    if (id == null) throw new IllegalArgumentException("id 不能为空");
    if (status == null) throw new IllegalArgumentException("status 不能为空");
  }
}
