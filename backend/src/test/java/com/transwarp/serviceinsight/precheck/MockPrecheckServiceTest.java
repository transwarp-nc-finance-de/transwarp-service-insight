package com.transwarp.serviceinsight.precheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.transwarp.serviceinsight.precheck.dto.PrecheckRequest;
import com.transwarp.serviceinsight.precheck.dto.ReferenceSourceType;
import com.transwarp.serviceinsight.precheck.infrastructure.mock.MockPrecheckAdapter;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockPrecheckServiceTest {
  private final MockPrecheckAdapter service = new MockPrecheckAdapter();

  @Test
  void returnsPredictableMockContentWithRequiredReviewAndReferences() {
    var response = service.precheck(request());
    assertThat(response.summary()).contains("模拟预诊").doesNotContain("最终根因是");
    assertThat(response.humanReviewRequired()).isTrue();
    assertThat(response.references()).hasSize(2).allMatch(reference -> reference.mockData());
    assertThat(response.references())
        .extracting("sourceType")
        .containsExactly(ReferenceSourceType.PRODUCT_MANUAL, ReferenceSourceType.HISTORICAL_SLA);
  }

  @Test
  void createsUniqueUuidForEveryInvocation() {
    var first = service.precheck(request()).precheckId();
    var second = service.precheck(request()).precheckId();
    assertThat(first).isNotEqualTo(second);
    assertThatCode(() -> UUID.fromString(first)).doesNotThrowAnyException();
  }

  private PrecheckRequest request() {
    return new PrecheckRequest("模拟问题", "模拟描述", "Inceptor", "SQL", "1.0", "P2", "单任务", "脱敏摘要");
  }
}
