package com.transwarp.serviceinsight.precheck.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transwarp.serviceinsight.precheck.application.ContinuePrecheckUseCase;
import com.transwarp.serviceinsight.precheck.application.ContinueSessionPrecheckUseCase;
import com.transwarp.serviceinsight.precheck.application.CreatePrecheckUseCase;
import com.transwarp.serviceinsight.precheck.application.GetPrecheckSessionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PrecheckController.class)
@Import(PrecheckApiMapper.class)
class PrecheckErrorTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private CreatePrecheckUseCase createPrecheck;
  @MockitoBean private ContinuePrecheckUseCase continuePrecheck;
  @MockitoBean private ContinueSessionPrecheckUseCase continueSession;
  @MockitoBean private GetPrecheckSessionUseCase getSession;

  @Test
  void hidesUnexpectedFailureDetails() throws Exception {
    when(createPrecheck.create(any()))
        .thenThrow(new IllegalStateException("sensitive stack detail"));
    mockMvc
        .perform(
            post("/api/v1/precheck")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"模拟标题\",\"description\":\"模拟描述\"}"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.fieldErrors").isEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.message").value("预诊服务暂时不可用，请稍后重试或继续人工提交"));
  }
}
