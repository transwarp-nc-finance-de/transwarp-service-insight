package com.transwarp.serviceinsight.precheck.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrecheckDomainTest {
  @Test
  void distinguishesSessionAndOrderedRuns() {
    var session = new PrecheckSession(UUID.randomUUID(), PrecheckStatus.RECEIVED);
    var run = new PrecheckRun(UUID.randomUUID(), session.id(), 2, PrecheckStatus.COMPLETED);

    assertThat(run.sessionId()).isEqualTo(session.id());
    assertThat(run.sequence()).isEqualTo(2);
    assertThat(run.id()).isNotEqualTo(session.id());
  }

  @Test
  void rejectsInvalidRunSequence() {
    assertThatThrownBy(
            () -> new PrecheckRun(UUID.randomUUID(), UUID.randomUUID(), 0, PrecheckStatus.RECEIVED))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sequence");
  }
}
