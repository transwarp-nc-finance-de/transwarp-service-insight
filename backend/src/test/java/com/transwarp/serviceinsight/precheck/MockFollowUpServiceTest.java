package com.transwarp.serviceinsight.precheck;

import static org.assertj.core.api.Assertions.assertThat;

import com.transwarp.serviceinsight.precheck.dto.Confidence;
import com.transwarp.serviceinsight.precheck.dto.FollowUpRequest;
import com.transwarp.serviceinsight.precheck.infrastructure.mock.MockPrecheckAdapter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MockFollowUpServiceTest {
  private final MockPrecheckAdapter service = new MockPrecheckAdapter();
  private final UUID precheckId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

  @ParameterizedTest
  @CsvSource({"这里有脱敏报错日志,日志或报错线索", "脱敏账号疑似权限不足,权限相关线索", "目前影响三个任务,影响范围线索", "昨晚进行过配置变更,近期变更线索"})
  void matchesDeterministicBranches(String message, String expected) {
    var response = service.followUp(new FollowUpRequest(precheckId, message));
    assertThat(response.reply()).contains("模拟数据", expected).doesNotContain("最终根因是");
    assertThat(response.precheckId()).isEqualTo(precheckId.toString());
    assertThat(response.confidence()).isEqualTo(Confidence.MEDIUM);
    assertThat(response.humanReviewRequired()).isTrue();
    assertThat(response.references()).allMatch(reference -> reference.mockData());
    assertThat(response.recommendations()).isNotEmpty();
    assertThat(response.missingInformation()).isNotEmpty();
    assertThat(response.fallbackReason()).contains("模拟数据", "未调用真实");
    assertThat(response.allowedActions()).contains("CONTINUE_SUBMISSION");
    assertThat(response.nextActionReason()).isNotBlank();
  }

  @Test
  void returnsLowConfidenceWhenInformationIsInsufficient() {
    var response = service.followUp(new FollowUpRequest(precheckId, "还有一个现象"));
    assertThat(response.confidence()).isEqualTo(Confidence.LOW);
    assertThat(response.reply()).contains("信息不足");
  }
}
