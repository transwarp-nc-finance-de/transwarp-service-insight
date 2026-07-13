package com.transwarp.serviceinsight.precheck.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PrecheckSessionControllerTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper mapper;

  @Test
  void createsRunOneThenRunTwoAndReturnsSessionState() throws Exception {
    var created =
        mockMvc
            .perform(
                post("/api/v1/precheck-sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"模拟问题\",\"description\":\"模拟数据：描述\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runSequence").value(1))
            .andReturn();
    var json = mapper.readTree(created.getResponse().getContentAsByteArray());
    var sessionId = json.get("sessionId").asText();
    mockMvc
        .perform(
            post("/api/v1/precheck-sessions/{sessionId}/runs", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"补充脱敏日志\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId").value(sessionId))
        .andExpect(jsonPath("$.runSequence").value(2));
    mockMvc
        .perform(get("/api/v1/precheck-sessions/{sessionId}", sessionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runs[0].sequence").value(1))
        .andExpect(jsonPath("$.runs[1].sequence").value(2));
  }

  @Test
  void returnsStructuredErrorForUnknownSession() throws Exception {
    mockMvc
        .perform(get("/api/v1/precheck-sessions/{sessionId}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }
}
