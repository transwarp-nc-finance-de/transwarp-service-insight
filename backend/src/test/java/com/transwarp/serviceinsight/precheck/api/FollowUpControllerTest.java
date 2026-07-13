package com.transwarp.serviceinsight.precheck.api;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transwarp.serviceinsight.precheck.domain.PrecheckSession;
import com.transwarp.serviceinsight.precheck.port.PrecheckSessionRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FollowUpControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private PrecheckSessionRepository sessions;

  @Test
  void returnsStructuredMockResponse() throws Exception {
    sessions.save(
        new PrecheckSession(
            UUID.randomUUID(), UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), "test"));
    mockMvc
        .perform(
            post("/api/v1/precheck/follow-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody("补充脱敏日志")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.followUpId", not(blankOrNullString())))
        .andExpect(jsonPath("$.precheckId").value("123e4567-e89b-12d3-a456-426614174000"))
        .andExpect(jsonPath("$.reply").value(org.hamcrest.Matchers.containsString("模拟数据")))
        .andExpect(jsonPath("$.recommendations").isArray())
        .andExpect(jsonPath("$.references[0].mockData").value(true))
        .andExpect(jsonPath("$.confidenceReason", not(blankOrNullString())))
        .andExpect(jsonPath("$.promptVersion").value("mock-rule-v1"))
        .andExpect(jsonPath("$.humanReviewRequired").value(true))
        .andExpect(jsonPath("$.missingInformation").isArray())
        .andExpect(jsonPath("$.fallbackReason", not(blankOrNullString())));
  }

  @Test
  void rejectsInvalidUuid() throws Exception {
    assertValidation("{\"precheckId\":\"invalid\",\"message\":\"模拟信息\"}");
  }

  @Test
  void rejectsBlankMessage() throws Exception {
    assertValidation(validBody("   "));
  }

  @Test
  void rejectsMessageAboveLimit() throws Exception {
    assertValidation(validBody("x".repeat(5001)));
  }

  private void assertValidation(String body) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/precheck/follow-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  private String validBody(String message) {
    return "{\"precheckId\":\"123e4567-e89b-12d3-a456-426614174000\",\"message\":\""
        + message
        + "\"}";
  }
}
