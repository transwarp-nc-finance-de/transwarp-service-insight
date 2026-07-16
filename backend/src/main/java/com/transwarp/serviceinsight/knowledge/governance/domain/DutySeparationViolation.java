package com.transwarp.serviceinsight.knowledge.governance.domain;

public class DutySeparationViolation extends RuntimeException {
  public DutySeparationViolation() {
    super("提交人与审核人必须是不同身份");
  }
}
