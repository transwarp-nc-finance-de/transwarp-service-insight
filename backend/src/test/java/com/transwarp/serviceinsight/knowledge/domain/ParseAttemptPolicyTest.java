package com.transwarp.serviceinsight.knowledge.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.transwarp.serviceinsight.knowledge.ingestion.domain.ParseAttemptPolicy;
import org.junit.jupiter.api.Test;

class ParseAttemptPolicyTest {
  private final ParseAttemptPolicy policy = new ParseAttemptPolicy();

  @Test
  void retriesOnlyTransientFailuresBeforeMaximumAttempt() {
    assertThat(policy.action(true, 1, 3)).isEqualTo(ParseAttemptPolicy.Action.RETRY);
    assertThat(policy.action(true, 2, 3)).isEqualTo(ParseAttemptPolicy.Action.RETRY);
    assertThat(policy.action(true, 3, 3)).isEqualTo(ParseAttemptPolicy.Action.FAIL);
    assertThat(policy.action(false, 1, 3)).isEqualTo(ParseAttemptPolicy.Action.FAIL);
  }
}
