package com.transwarp.serviceinsight.knowledge.ingestion.domain;

import org.springframework.stereotype.Component;

@Component
public class ParseAttemptPolicy {
  public enum Action {
    RETRY,
    FAIL
  }

  public Action action(boolean retryable, int attempt, int maxAttempts) {
    return retryable && attempt < maxAttempts ? Action.RETRY : Action.FAIL;
  }
}
