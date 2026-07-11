package com.transwarp.serviceinsight.precheck.api;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        .andExpect(jsonPath("$.precheckId", not(blankOrNullString())))
        .andExpect(jsonPath("$.humanReviewRequired").value(true))
        .andExpect(jsonPath("$.references[0].mockData").value(true));
  }

  @Test
  void rejectsMissingTitle() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/precheck")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"模拟描述\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.title").exists())
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
