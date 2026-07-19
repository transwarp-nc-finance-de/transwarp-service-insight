package com.transwarp.serviceinsight.knowledge.publication.domain;

import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class IndexAttemptPolicy {
  public boolean shouldRetry(boolean retryable, int attempt, int maxAttempts) {
    return retryable && attempt < maxAttempts;
  }

  public Duration retryDelay(int attempt) {
    if (attempt < 1) throw new IllegalArgumentException("attempt must be positive");
    return Duration.ofMillis(100L << Math.min(attempt - 1, 2));
  }
}
