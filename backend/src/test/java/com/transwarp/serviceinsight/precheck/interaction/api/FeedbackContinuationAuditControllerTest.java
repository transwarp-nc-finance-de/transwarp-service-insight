package com.transwarp.serviceinsight.precheck.interaction.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.knowledge.publication.port.EmbeddingPort;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackContinuationAuditControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;
  @MockitoBean private EmbeddingPort embeddingPort;

  @BeforeEach
  void embeddingAvailable() {
    when(embeddingPort.embedQueries(anyList()))
        .thenAnswer(
            invocation ->
                ((List<?>) invocation.getArgument(0))
                    .stream().map(ignored -> new float[768]).toList());
  }

  @Test
  void feedbackIsPersistedWithoutTerminatingTheSession() throws Exception {
    var login = login("mock-precheck-tdh");
    var session = createSession(login, "mock-issue-24-feedback-001");
    var sessionJson = objectMapper.readTree(session.getResponse().getContentAsByteArray());
    var sessionId = sessionJson.path("sessionId").asText();
    var runId = sessionJson.at("/latestRun/runId").asText();

    var created =
        mockMvc
            .perform(
                post("/api/v2/feedback")
                    .cookie(login.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "issue24-feedback-command-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"sessionId":"%s","runId":"%s","adoptionStatus":"PARTIALLY_ADOPTED"}
                        """
                            .formatted(sessionId, runId)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Idempotency-Replayed", "false"))
            .andExpect(jsonPath("$.sessionId").value(sessionId))
            .andExpect(jsonPath("$.runId").value(runId))
            .andExpect(jsonPath("$.adoptionStatus").value("PARTIALLY_ADOPTED"))
            .andExpect(jsonPath("$.helpfulness").isEmpty())
            .andExpect(jsonPath("$.reason").isEmpty())
            .andReturn();
    var feedbackId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("feedbackId")
            .asText();

    mockMvc
        .perform(
            get("/api/v2/feedback/{feedbackId}", feedbackId)
                .cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.feedbackId").value(feedbackId))
        .andExpect(jsonPath("$.adoptionStatus").value("PARTIALLY_ADOPTED"));
    mockMvc
        .perform(
            get("/api/v2/precheck-sessions/{sessionId}", sessionId)
                .cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.terminationReason").isEmpty());
  }

  @Test
  void continuationTerminatesTheSessionWithoutCreatingATicket() throws Exception {
    var login = login("mock-precheck-tdh");
    var session = createSession(login, "mock-issue-24-continuation-001");
    var sessionId =
        objectMapper
            .readTree(session.getResponse().getContentAsByteArray())
            .path("sessionId")
            .asText();

    var created =
        mockMvc
            .perform(
                post("/api/v2/submission-continuations")
                    .cookie(login.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "issue24-continuation-command-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"sessionId":"%s","confirmed":true,"reason":"模拟数据：由人工继续提交"}
                        """
                            .formatted(sessionId)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Idempotency-Replayed", "false"))
            .andExpect(jsonPath("$.sessionId").value(sessionId))
            .andExpect(jsonPath("$.confirmedBy").value("mock-precheck-tdh"))
            .andExpect(jsonPath("$.reason").value("模拟数据：由人工继续提交"))
            .andExpect(jsonPath("$.ticketId").doesNotExist())
            .andExpect(jsonPath("$.draft").doesNotExist())
            .andExpect(jsonPath("$.receipt").doesNotExist())
            .andReturn();
    var continuationId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("continuationId")
            .asText();

    mockMvc
        .perform(
            get("/api/v2/submission-continuations/{continuationId}", continuationId)
                .cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.continuationId").value(continuationId));
    mockMvc
        .perform(
            get("/api/v2/precheck-sessions/{sessionId}", sessionId)
                .cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("TERMINATED"))
        .andExpect(jsonPath("$.terminationReason").value("CONTINUED_SUBMISSION"));
  }

  @Test
  void adminReadsRedactedStructuredAuditEvents() throws Exception {
    var user = login("mock-precheck-tdh");
    var session = createSession(user, "mock-issue-24-audit-001");
    var sessionJson = objectMapper.readTree(session.getResponse().getContentAsByteArray());
    var sessionId = sessionJson.path("sessionId").asText();
    var runId = sessionJson.at("/latestRun/runId").asText();
    var feedbackReason = "模拟数据：敏感反馈原因 AUDIT-SECRET-2401";
    var continuationReason = "模拟数据：敏感继续原因 AUDIT-SECRET-2402";

    var feedback =
        mockMvc
            .perform(
                post("/api/v2/feedback")
                    .cookie(user.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", user.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "issue24-audit-feedback-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"sessionId":"%s","runId":"%s","adoptionStatus":"ADOPTED","helpfulness":"HELPFUL","reason":"%s"}
                        """
                            .formatted(sessionId, runId, feedbackReason)))
            .andExpect(status().isCreated())
            .andReturn();
    var feedbackId =
        objectMapper
            .readTree(feedback.getResponse().getContentAsByteArray())
            .path("feedbackId")
            .asText();
    var continuation =
        mockMvc
            .perform(
                post("/api/v2/submission-continuations")
                    .cookie(user.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", user.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "issue24-audit-continuation-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"sessionId":"%s","confirmed":true,"reason":"%s"}
                        """
                            .formatted(sessionId, continuationReason)))
            .andExpect(status().isCreated())
            .andReturn();
    var continuationId =
        objectMapper
            .readTree(continuation.getResponse().getContentAsByteArray())
            .path("continuationId")
            .asText();

    var admin = login("mock-admin");
    var response =
        mockMvc
            .perform(
                get("/api/v2/audit-events")
                    .cookie(admin.getResponse().getCookie("SESSION"))
                    .queryParam("size", "100")
                    .queryParam("sortDirection", "ASC"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.items[*].actorUserCode")
                    .value(org.hamcrest.Matchers.hasItem("mock-precheck-tdh")))
            .andExpect(
                jsonPath("$.items[*].action")
                    .value(
                        org.hamcrest.Matchers.hasItems(
                            "FEEDBACK_RECORDED", "SUBMISSION_CONTINUED")))
            .andExpect(
                jsonPath("$.items[*].outcome")
                    .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("SUCCEEDED"))))
            .andExpect(
                jsonPath("$.items[*].metadata.productLineCode")
                    .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("TDH"))))
            .andExpect(
                jsonPath("$.items[*].subjectId")
                    .value(org.hamcrest.Matchers.hasItems(feedbackId, continuationId)))
            .andReturn();
    var body = response.getResponse().getContentAsString();
    org.assertj.core.api.Assertions.assertThat(body)
        .doesNotContain(feedbackReason)
        .doesNotContain(continuationReason)
        .doesNotContain("Issue 24 反馈")
        .doesNotContain("MOCK-2401");
  }

  @Test
  void commandsAreIndependentlyIdempotentAndRejectChangedPayloads() throws Exception {
    var login = login("mock-precheck-tdh");
    var session = createSession(login, "mock-issue-24-idempotency-001");
    var sessionJson = objectMapper.readTree(session.getResponse().getContentAsByteArray());
    var sessionId = sessionJson.path("sessionId").asText();
    var runId = sessionJson.at("/latestRun/runId").asText();
    var feedbackBody =
        """
        {"sessionId":"%s","runId":"%s","adoptionStatus":"ADOPTED"}
        """
            .formatted(sessionId, runId);

    var firstFeedback =
        postFeedback(login, "issue24-idempotent-feedback-001", feedbackBody)
            .andExpect(status().isCreated())
            .andReturn();
    var feedbackId =
        objectMapper
            .readTree(firstFeedback.getResponse().getContentAsByteArray())
            .path("feedbackId")
            .asText();
    postFeedback(login, "issue24-idempotent-feedback-001", feedbackBody)
        .andExpect(status().isOk())
        .andExpect(header().string("Idempotency-Replayed", "true"))
        .andExpect(jsonPath("$.feedbackId").value(feedbackId));
    postFeedback(
            login, "issue24-idempotent-feedback-001", feedbackBody.replace("ADOPTED", "IGNORED"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));

    var continuationBody =
        """
        {"sessionId":"%s","confirmed":true}
        """
            .formatted(sessionId);
    var firstContinuation =
        postContinuation(login, "issue24-idempotent-continuation-001", continuationBody)
            .andExpect(status().isCreated())
            .andReturn();
    var continuationId =
        objectMapper
            .readTree(firstContinuation.getResponse().getContentAsByteArray())
            .path("continuationId")
            .asText();
    postContinuation(login, "issue24-idempotent-continuation-001", continuationBody)
        .andExpect(status().isOk())
        .andExpect(header().string("Idempotency-Replayed", "true"))
        .andExpect(jsonPath("$.continuationId").value(continuationId));
    postContinuation(
            login,
            "issue24-idempotent-continuation-001",
            """
            {"sessionId":"%s","confirmed":true,"reason":"changed"}
            """
                .formatted(sessionId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
  }

  @Test
  void invalidFeedbackDoesNotBlockContinuationAndValidationCanBeRetried() throws Exception {
    var login = login("mock-precheck-tdh");
    var session = createSession(login, "mock-issue-24-independent-failure-001");
    var sessionJson = objectMapper.readTree(session.getResponse().getContentAsByteArray());
    var sessionId = sessionJson.path("sessionId").asText();
    var runId = sessionJson.at("/latestRun/runId").asText();

    postFeedback(
            login,
            "issue24-independent-feedback-001",
            """
            {"sessionId":"%s","runId":"%s","adoptionStatus":"UNKNOWN"}
            """
                .formatted(sessionId, runId))
        .andExpect(status().isBadRequest());
    postContinuation(
            login,
            "issue24-independent-continuation-001",
            """
            {"sessionId":"%s","confirmed":true}
            """
                .formatted(sessionId))
        .andExpect(status().isCreated());

    var retrySession = createSession(login, "mock-issue-24-validation-retry-001");
    var retryJson = objectMapper.readTree(retrySession.getResponse().getContentAsByteArray());
    var retrySessionId = retryJson.path("sessionId").asText();
    var retryRunId = retryJson.at("/latestRun/runId").asText();
    postFeedback(
            login,
            "issue24-validation-retry-feedback-001",
            """
            {"sessionId":"%s","runId":"%s"}
            """
                .formatted(retrySessionId, retryRunId))
        .andExpect(status().isBadRequest());
    postFeedback(
            login,
            "issue24-validation-retry-feedback-001",
            """
            {"sessionId":"%s","runId":"%s","adoptionStatus":"IGNORED"}
            """
                .formatted(retrySessionId, retryRunId))
        .andExpect(status().isCreated());
  }

  @Test
  void adminCannotContinueAndUnknownRecordsUseSafeNotFound() throws Exception {
    var admin = login("mock-admin");
    postContinuation(
            admin,
            "issue24-admin-continuation-001",
            """
            {"sessionId":"%s","confirmed":true}
            """
                .formatted(UUID.randomUUID()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INSUFFICIENT_ROLE"));

    var user = login("mock-precheck-tdh");
    mockMvc
        .perform(
            get("/api/v2/feedback/{feedbackId}", UUID.randomUUID())
                .cookie(user.getResponse().getCookie("SESSION")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void feedbackContinuationAndAuditHistoryAreDatabaseImmutable() throws Exception {
    var login = login("mock-precheck-tdh");
    var session = createSession(login, "mock-issue-24-immutable-001");
    var sessionJson = objectMapper.readTree(session.getResponse().getContentAsByteArray());
    var sessionId = sessionJson.path("sessionId").asText();
    var runId = sessionJson.at("/latestRun/runId").asText();
    var feedback =
        postFeedback(
                login,
                "issue24-immutable-feedback-001",
                """
                {"sessionId":"%s","runId":"%s","adoptionStatus":"ADOPTED"}
                """
                    .formatted(sessionId, runId))
            .andExpect(status().isCreated())
            .andReturn();
    var feedbackId =
        objectMapper
            .readTree(feedback.getResponse().getContentAsByteArray())
            .path("feedbackId")
            .asText();
    var continuation =
        postContinuation(
                login,
                "issue24-immutable-continuation-001",
                """
                {"sessionId":"%s","confirmed":true}
                """
                    .formatted(sessionId))
            .andExpect(status().isCreated())
            .andReturn();
    var continuationJson =
        objectMapper.readTree(continuation.getResponse().getContentAsByteArray());
    var continuationId = continuationJson.path("continuationId").asText();
    var auditEventId = continuationJson.path("auditEventId").asText();

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE precheck_feedback_v2 SET adoption_status = 'IGNORED' WHERE feedback_id = ?",
                    UUID.fromString(feedbackId)))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM submission_continuation_v2 WHERE continuation_id = ?",
                    UUID.fromString(continuationId)))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE audit_event_v2 SET outcome = 'FAILED' WHERE event_id = ?",
                    UUID.fromString(auditEventId)))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
  }

  @Test
  void concurrentFeedbackRetriesCreateOnlyOneRecord() throws Exception {
    var login = login("mock-precheck-tdh");
    var session = createSession(login, "mock-issue-24-concurrent-feedback-001");
    var sessionJson = objectMapper.readTree(session.getResponse().getContentAsByteArray());
    var body =
        """
        {"sessionId":"%s","runId":"%s","adoptionStatus":"ADOPTED"}
        """
            .formatted(
                sessionJson.path("sessionId").asText(),
                sessionJson.at("/latestRun/runId").asText());
    var gate = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(
              () -> {
                gate.await();
                return postFeedback(login, "issue24-concurrent-feedback-001", body).andReturn();
              });
      var second =
          executor.submit(
              () -> {
                gate.await();
                return postFeedback(login, "issue24-concurrent-feedback-001", body).andReturn();
              });
      gate.countDown();
      var responses = List.of(first.get(), second.get());
      org.assertj.core.api.Assertions.assertThat(
              responses.stream().map(value -> value.getResponse().getStatus()).toList())
          .containsExactlyInAnyOrder(200, 201);
      org.assertj.core.api.Assertions.assertThat(
              responses.stream()
                  .map(
                      value -> {
                        try {
                          return objectMapper
                              .readTree(value.getResponse().getContentAsByteArray())
                              .path("feedbackId")
                              .asText();
                        } catch (java.io.IOException exception) {
                          throw new IllegalStateException(exception);
                        }
                      })
                  .distinct()
                  .toList())
          .hasSize(1);
    }
  }

  private org.springframework.test.web.servlet.ResultActions postFeedback(
      MvcResult login, String idempotencyKey, String body) throws Exception {
    return mockMvc.perform(
        post("/api/v2/feedback")
            .cookie(login.getResponse().getCookie("SESSION"))
            .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private org.springframework.test.web.servlet.ResultActions postContinuation(
      MvcResult login, String idempotencyKey, String body) throws Exception {
    return mockMvc.perform(
        post("/api/v2/submission-continuations")
            .cookie(login.getResponse().getCookie("SESSION"))
            .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
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

  private MvcResult createSession(MvcResult login, String hostRequestId) throws Exception {
    return mockMvc
        .perform(
            post("/api/v2/precheck-sessions")
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionRequest(hostRequestId)))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private String sessionRequest(String hostRequestId) {
    return """
        {"context":{"sourceSystem":"SANDBOX","hostRequestId":"%s","formSchemaVersion":"1.0","issueType":{"code":"FUNCTIONAL_FAILURE","displayName":"功能故障（模拟数据）"},"productLine":{"code":"TDH","displayName":"TDH（模拟数据）"},"product":{"code":"INCEPTOR","displayName":"Inceptor（模拟数据）"},"component":{"code":"SQL_ENGINE","displayName":"SQL 引擎（模拟数据）"},"version":"9.1.0-mock","severity":{"code":"S2","displayName":"S2（模拟数据）"},"serviceType":{"code":"CONSULTATION","displayName":"咨询（模拟数据）"},"title":"模拟数据：Issue 24 反馈","descriptionPlainText":"模拟数据：错误码 MOCK-2401。","additionalInformation":[],"impactScope":"模拟数据：单个任务","attachments":[]}}
        """
        .formatted(hostRequestId);
  }
}
