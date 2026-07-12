package com.transwarp.serviceinsight.feedback.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transwarp.serviceinsight.audit.domain.BusinessEventType;
import com.transwarp.serviceinsight.audit.infrastructure.memory.InMemoryAuditAdapter;
import com.transwarp.serviceinsight.feedback.infrastructure.memory.InMemoryFeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private InMemoryFeedbackRepository repository;
  @Autowired private InMemoryAuditAdapter audit;

  @BeforeEach
  void clearMemoryAdapters() {
    repository.clear();
    audit.clear();
  }

  @Test
  void recordsFeedbackAndRedactedBusinessEvents() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/precheck/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "precheckId":"123e4567-e89b-12d3-a456-426614174000",
                      "adoptionStatus":"PARTIALLY_ADOPTED",
                      "feedbackReason":"模拟数据：人工仍需核对",
                      "continuedSubmission":true
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recorded").value(true))
        .andExpect(jsonPath("$.mockData").value(true))
        .andExpect(jsonPath("$.policyVersion").value("mock-policy-v1"))
        .andExpect(jsonPath("$.continuedSubmission").value(true));

    assertThat(repository.snapshot()).hasSize(1);
    assertThat(audit.snapshot())
        .extracting(event -> event.type())
        .containsExactly(
            BusinessEventType.FEEDBACK_RECORDED, BusinessEventType.TICKET_SUBMISSION_CONTINUED);
    assertThat(audit.snapshot())
        .allSatisfy(
            event -> {
              assertThat(event.subjectId()).isEqualTo("mock-user");
              assertThat(event.policyVersion()).isEqualTo("mock-policy-v1");
              assertThat(event.metadata()).doesNotContainKey("feedbackReason");
              assertThat(event.metadata().toString()).doesNotContain("人工仍需核对");
            });
  }

  @Test
  void rejectsInvalidFeedback() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/precheck/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"precheckId\":\"bad\",\"adoptionStatus\":\"UNKNOWN\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
