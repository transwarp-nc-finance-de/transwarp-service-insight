package com.transwarp.serviceinsight.precheck.application;

import java.util.UUID;

public class PrecheckSessionIdNotFoundException extends RuntimeException {
  public PrecheckSessionIdNotFoundException(UUID id) {
    super("未找到预诊会话：" + id);
  }
}
