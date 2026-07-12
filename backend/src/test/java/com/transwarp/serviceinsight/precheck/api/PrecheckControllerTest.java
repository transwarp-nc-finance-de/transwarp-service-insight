package com.transwarp.serviceinsight.precheck.api;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PrecheckControllerTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void returnsSuccessfulContract() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/precheck").contentType(MediaType.APPLICATION_JSON).content(validBody()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.precheckId", not(blankOrNullString())))
        .andExpect(jsonPath("$.sessionId", not(blankOrNullString())))
        .andExpect(jsonPath("$.summary", not(blankOrNullString())))
        .andExpect(jsonPath("$.recommendations").isArray())
        .andExpect(jsonPath("$.references").isArray())
        .andExpect(jsonPath("$.confidence", matchesPattern("HIGH|MEDIUM|LOW")))
        .andExpect(jsonPath("$.confidenceReason", not(blankOrNullString())))
        .andExpect(jsonPath("$.humanReviewRequired").value(true))
        .andExpect(jsonPath("$.references[0].mockData").value(true))
        .andExpect(jsonPath("$.missingInformation").isArray())
        .andExpect(jsonPath("$.fallbackReason", not(blankOrNullString())))
        .andExpect(jsonPath("$.nextAction", not(blankOrNullString())))
        .andExpect(jsonPath("$.nextActionReason", not(blankOrNullString())))
        .andExpect(jsonPath("$.allowedActions").isArray())
        .andExpect(jsonPath("$.allowedActions[1]").value("CONTINUE_SUBMISSION"))
        .andExpect(jsonPath("$.status", not(blankOrNullString())))
        .andExpect(jsonPath("$.policyVersion").value("mock-policy-v1"))
        .andExpect(jsonPath("$.modelVersion").value("not-applicable-deterministic-mock"))
        .andExpect(jsonPath("$.promptVersion").value("mock-rule-v1"))
        .andExpect(jsonPath("$.indexVersion").value("not-applicable-no-index"));
  }

  @Test
  void rejectsMissingTitle() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/precheck")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"模拟描述\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("请求参数校验失败"))
        .andExpect(jsonPath("$.fieldErrors.title").exists())
        .andExpect(jsonPath("$.timestamp", not(blankOrNullString())))
        .andExpect(jsonPath("$.traceId", not(blankOrNullString())));
  }

  @Test
  void rejectsMissingDescription() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/precheck")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"模拟标题\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.description").exists());
  }

  @Test
  void rejectsLengthAboveLimit() throws Exception {
    var body = "{\"title\":\"" + "x".repeat(201) + "\",\"description\":\"模拟描述\"}";
    mockMvc
        .perform(post("/api/v1/precheck").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  private String validBody() {
    return "{\"title\":\"模拟标题\",\"description\":\"模拟描述\",\"severity\":\"P2\"}";
  }
}
