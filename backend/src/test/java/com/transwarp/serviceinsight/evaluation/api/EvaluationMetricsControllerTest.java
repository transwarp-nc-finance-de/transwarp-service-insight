package com.transwarp.serviceinsight.evaluation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:evaluation_metrics_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class EvaluationMetricsControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void adminCreatesAPersistentAsynchronousEvaluationRun() throws Exception {
    var login = login("mock-admin");

    mockMvc
        .perform(
            post("/api/v2/evaluation-runs")
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "evaluation-run-command-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"evaluationSetVersion":"mock-eval-v1","note":"模拟数据：Issue #27 回归验证"}
                    """))
        .andExpect(status().isAccepted())
        .andExpect(header().string("Idempotency-Replayed", "false"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.attempt").value(0))
        .andExpect(jsonPath("$.maxAttempts").value(3))
        .andExpect(jsonPath("$.evaluationSetVersion").value("mock-eval-v1"))
        .andExpect(jsonPath("$.summary").isEmpty())
        .andExpect(jsonPath("$.mockData").value(true));
  }

  @Test
  void createIsAdminOnlyAndUsesContractIdempotency() throws Exception {
    var user = login("mock-precheck-tdh");
    mockMvc
        .perform(
            post("/api/v2/evaluation-runs")
                .cookie(user.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", user.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "evaluation-role-command-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"evaluationSetVersion\":\"mock-eval-v1\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INSUFFICIENT_ROLE"));

    var admin = login("mock-admin");
    for (int request = 0; request < 2; request++) {
      mockMvc
          .perform(
              post("/api/v2/evaluation-runs")
                  .cookie(admin.getResponse().getCookie("SESSION"))
                  .header("X-CSRF-Token", admin.getResponse().getHeader("X-CSRF-Token"))
                  .header("Idempotency-Key", "evaluation-replay-command-001")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"evaluationSetVersion\":\"mock-eval-v1\"}"))
          .andExpect(status().is(request == 0 ? 202 : 200))
          .andExpect(header().string("Idempotency-Replayed", request == 0 ? "false" : "true"));
    }
    mockMvc
        .perform(
            post("/api/v2/evaluation-runs")
                .cookie(admin.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", admin.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "evaluation-replay-command-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"evaluationSetVersion\":\"mock-eval-v1\",\"note\":\"different\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
  }

  @Test
  void adminReadsThePersistentEvaluationRunByTaskId() throws Exception {
    var login = login("mock-admin");
    var created =
        mockMvc
            .perform(
                post("/api/v2/evaluation-runs")
                    .cookie(login.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "evaluation-read-command-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"evaluationSetVersion\":\"mock-eval-v1\"}"))
            .andExpect(status().isAccepted())
            .andReturn();
    var taskId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("taskId")
            .asText();

    var completed = awaitTerminal(taskId, login);
    mockMvc
        .perform(
            get("/api/v2/evaluation-runs/{taskId}", taskId)
                .cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.taskId").value(taskId))
        .andExpect(jsonPath("$.status").value("SUCCEEDED"))
        .andExpect(jsonPath("$.evaluationSetVersion").value("mock-eval-v1"))
        .andExpect(jsonPath("$.summary.sampleCount").value(30))
        .andExpect(jsonPath("$.summary.permissionLeakageRate").value(0.0))
        .andExpect(jsonPath("$.summary.citationErrorRate").value(0.0))
        .andExpect(jsonPath("$.summary.degradationPassRate").value(1.0))
        .andExpect(jsonPath("$.summary.recallAt5").isNumber())
        .andExpect(jsonPath("$.summary.disclaimer").value("小样本工程评估，不代表生产效果"));
  }

  @Test
  void adminListsSafeFailuresAndMetricsExcludeEvaluationTraces() throws Exception {
    var login = login("mock-admin");
    var created =
        mockMvc
            .perform(
                post("/api/v2/evaluation-runs")
                    .cookie(login.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "evaluation-report-command-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"evaluationSetVersion\":\"mock-eval-v1\"}"))
            .andExpect(status().isAccepted())
            .andReturn();
    var taskId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("taskId")
            .asText();
    awaitTerminal(taskId, login);

    mockMvc
        .perform(
            get("/api/v2/evaluation-runs")
                .param("status", "SUCCEEDED")
                .cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].status").value("SUCCEEDED"))
        .andExpect(jsonPath("$.page.totalItems").isNumber());

    mockMvc
        .perform(
            get("/api/v2/evaluation-runs/{taskId}/failures", taskId)
                .cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].caseId").exists())
        .andExpect(jsonPath("$.items[0].actual.unauthorizedEvidenceCount").isNumber())
        .andExpect(jsonPath("$.items[0].input").doesNotExist())
        .andExpect(jsonPath("$.items[0].evidenceIds").doesNotExist());

    mockMvc
        .perform(
            get("/api/v2/metrics")
                .param("from", "2026-01-01T00:00:00Z")
                .param("to", "2027-01-01T00:00:00Z")
                .cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.precheckCount").value(0))
        .andExpect(jsonPath("$.retrievalP95Ms").value(0))
        .andExpect(jsonPath("$.mockData").value(true));
  }

  private MvcResult awaitTerminal(String taskId, MvcResult login) throws Exception {
    MvcResult result = null;
    for (int attempt = 0; attempt < 100; attempt++) {
      result =
          mockMvc
              .perform(
                  get("/api/v2/evaluation-runs/{taskId}", taskId)
                      .cookie(login.getResponse().getCookie("SESSION")))
              .andExpect(status().isOk())
              .andReturn();
      var status =
          objectMapper
              .readTree(result.getResponse().getContentAsByteArray())
              .path("status")
              .asText();
      if ("SUCCEEDED".equals(status) || "FAILED".equals(status)) return result;
      Thread.sleep(20);
    }
    throw new AssertionError("evaluation run did not reach a terminal state");
  }

  private MvcResult login(String userCode) throws Exception {
    return mockMvc
        .perform(
            post("/api/v2/auth-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userCode\":\"" + userCode + "\",\"mockData\":true}"))
        .andExpect(status().isCreated())
        .andReturn();
  }
}
