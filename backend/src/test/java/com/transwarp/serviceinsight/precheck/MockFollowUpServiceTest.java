package com.transwarp.serviceinsight.precheck;

import static org.assertj.core.api.Assertions.assertThat;

import com.transwarp.serviceinsight.precheck.infrastructure.mock.MockPrecheckAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MockFollowUpServiceTest {
  private final MockPrecheckAdapter adapter = new MockPrecheckAdapter();

  @ParameterizedTest
  @ValueSource(strings = {"补充脱敏日志", "账号权限不足", "影响三个任务", "昨晚配置变更"})
  void matchesDeterministicBranches(String message) {
    var response = adapter.followUp(message);
    assertThat(response.matched()).isTrue();
    assertThat(response.reply()).contains("模拟数据");
    assertThat(response.missingInformation()).isNotEmpty();
  }

  @Test
  void returnsInsufficientInformation() {
    assertThat(adapter.followUp("还有一个现象").matched()).isFalse();
  }
}
