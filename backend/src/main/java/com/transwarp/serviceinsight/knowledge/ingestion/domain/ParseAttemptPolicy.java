package com.transwarp.serviceinsight.knowledge.ingestion.domain;

import java.time.Duration;
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

  public Duration retryDelay(int attempt) {
    if (attempt < 1) throw new IllegalArgumentException("attempt must be positive");
    return Duration.ofMillis(100L << Math.min(attempt - 1, 2));
  }
}
