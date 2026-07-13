package com.transwarp.serviceinsight.precheck;

import static org.assertj.core.api.Assertions.assertThat;

import com.transwarp.serviceinsight.precheck.application.CreatePrecheckCommand;
import com.transwarp.serviceinsight.precheck.infrastructure.mock.MockPrecheckAdapter;
import org.junit.jupiter.api.Test;

class MockPrecheckServiceTest {
  private final MockPrecheckAdapter adapter = new MockPrecheckAdapter();

  @Test
  void returnsMissingInformationAndMockEvidence() {
    var command =
        new CreatePrecheckCommand("模拟问题", "模拟描述", "Inceptor", "SQL", null, "P2", null, null);
    assertThat(adapter.missingInformation(command)).contains("版本", "影响范围");
    assertThat(adapter.verify(adapter.search("模拟查询"))).hasSize(2).allMatch(item -> item.mockData());
  }
}
