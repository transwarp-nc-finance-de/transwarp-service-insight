package com.transwarp.serviceinsight.precheck.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transwarp.serviceinsight.precheck.MockPrecheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PrecheckController.class)
class PrecheckErrorTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private MockPrecheckService service;

  @Test
  void hidesUnexpectedFailureDetails() throws Exception {
    when(service.precheck(any())).thenThrow(new IllegalStateException("sensitive stack detail"));
    mockMvc
        .perform(
            post("/api/v1/precheck")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"模拟标题\",\"description\":\"模拟描述\"}"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.message").value("预诊服务暂时不可用，请稍后重试或继续人工提交"));
  }
}
