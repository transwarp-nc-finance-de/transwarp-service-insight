package com.transwarp.serviceinsight.precheck.domain;

public enum PrecheckStatus {
  RECEIVED,
  VALIDATED,
  COMPLETED,
  NEED_MORE_INFORMATION,
  DEGRADED,
  FAILED,
  CANCELLED
}
