package com.transwarp.serviceinsight.knowledge.governance.domain;

public class IllegalGovernanceTransition extends RuntimeException {
  public IllegalGovernanceTransition(String message) {
    super(message);
  }
}
