package com.transwarp.serviceinsight.precheck.application;

import java.util.UUID;

public class PrecheckSessionNotFoundException extends RuntimeException {
  public PrecheckSessionNotFoundException(UUID precheckId) {
    super("未找到预诊会话：" + precheckId);
  }
}
