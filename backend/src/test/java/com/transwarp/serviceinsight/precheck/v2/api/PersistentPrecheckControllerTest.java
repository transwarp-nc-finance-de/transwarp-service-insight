package com.transwarp.serviceinsight.precheck.v2.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PersistentPrecheckControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void precheckUserCreatesSessionWithPersistentRunOneAndSafeDegradedResult() throws Exception {
    var login = login("mock-precheck-tdh");

    mockMvc
        .perform(
            post("/api/v2/precheck-sessions")
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionRequest("mock-host-create-001", "模拟数据：查询失败")))
        .andExpect(status().isCreated())
        .andExpect(header().string("Idempotency-Replayed", "false"))
        .andExpect(jsonPath("$.ownerUserCode").value("mock-precheck-tdh"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.runCount").value(1))
        .andExpect(jsonPath("$.maxRuns").value(3))
        .andExpect(jsonPath("$.latestRun.sequence").value(1))
        .andExpect(
            jsonPath("$.latestRun.contextSnapshot.hostRequestId").value("mock-host-create-001"))
        .andExpect(
            jsonPath("$.latestRun.result.completeness.policyVersion").value("mock-completeness-v1"))
        .andExpect(jsonPath("$.latestRun.result.confidence").value("LOW"))
        .andExpect(jsonPath("$.latestRun.result.confidenceReasons[0]").isNotEmpty())
        .andExpect(jsonPath("$.latestRun.result.humanInterventionAdvice[0]").isNotEmpty())
        .andExpect(jsonPath("$.latestRun.result.missingInformation").isArray())
        .andExpect(
            jsonPath("$.latestRun.result.allowedActions")
                .value(org.hamcrest.Matchers.hasItem("CONTINUE_SUBMISSION")))
        .andExpect(jsonPath("$.latestRun.result.retrieval.mode").value("UNAVAILABLE"))
        .andExpect(jsonPath("$.latestRun.result.retrieval.degraded").value(true))
        .andExpect(jsonPath("$.latestRun.result.evidence").isEmpty())
        .andExpect(
            jsonPath("$.latestRun.result.disclaimer")
                .value(org.hamcrest.Matchers.containsString("不是最终根因")))
        .andExpect(jsonPath("$.latestRun.result.mockData").value(true))
        .andExpect(jsonPath("$.mockData").value(true));
  }

  @Test
  void fullContextCreatesIdempotentNextRun() throws Exception {
    var login = login("mock-precheck-tdh");
    var created =
        mockMvc
            .perform(
                post("/api/v2/precheck-sessions")
                    .cookie(login.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(sessionRequest("mock-host-run-001", "模拟数据：初始标题")))
            .andExpect(status().isCreated())
            .andReturn();
    var sessionId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("sessionId")
            .asText();
    var nextContext = sessionRequest("mock-host-run-001", "模拟数据：补充后的标题");

    var next =
        mockMvc
            .perform(
                post("/api/v2/precheck-sessions/{sessionId}/runs", sessionId)
                    .cookie(login.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "precheck-run-command-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(nextContext))
            .andExpect(status().isCreated())
            .andExpect(header().string("Idempotency-Replayed", "false"))
            .andExpect(jsonPath("$.sequence").value(2))
            .andExpect(jsonPath("$.contextSnapshot.title").value("模拟数据：补充后的标题"))
            .andExpect(
                jsonPath("$.result.allowedActions")
                    .value(org.hamcrest.Matchers.hasItem("CONTINUE_SUBMISSION")))
            .andReturn();
    var runId =
        objectMapper.readTree(next.getResponse().getContentAsByteArray()).path("runId").asText();

    mockMvc
        .perform(
            post("/api/v2/precheck-sessions/{sessionId}/runs", sessionId)
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "precheck-run-command-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nextContext))
        .andExpect(status().isOk())
        .andExpect(header().string("Idempotency-Replayed", "true"))
        .andExpect(jsonPath("$.runId").value(runId));
  }

  @Test
  void explicitSelfServiceConfirmationTerminatesSessionAndMakesItReadOnly() throws Exception {
    var login = login("mock-precheck-tdh");
    var created = createSession(login, "mock-host-terminate-001");
    var sessionId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("sessionId")
            .asText();

    mockMvc
        .perform(
            post("/api/v2/precheck-sessions/{sessionId}/self-service-confirmations", sessionId)
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "self-service-command-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmed\":true,\"reason\":\"模拟数据：已由人工确认采用自助建议\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("TERMINATED"))
        .andExpect(jsonPath("$.reason").value("SELF_SERVICE_CONFIRMED"))
        .andExpect(jsonPath("$.auditEventId").isNotEmpty())
        .andExpect(jsonPath("$.mockData").value(true));

    mockMvc
        .perform(
            post("/api/v2/precheck-sessions/{sessionId}/runs", sessionId)
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "post-termination-run-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionRequest("mock-host-terminate-001", "模拟数据：终止后修改")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ILLEGAL_STATE_TRANSITION"));
  }

  @Test
  void ownerRestoresSessionAndRunHistoryButAdminCannotTakeItOver() throws Exception {
    var owner = login("mock-precheck-tdh");
    var created = createSession(owner, "mock-host-restore-001");
    var sessionId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("sessionId")
            .asText();

    mockMvc
        .perform(get("/api/v2/precheck-sessions").cookie(owner.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].sessionId").value(sessionId));
    mockMvc
        .perform(
            get("/api/v2/precheck-sessions/{sessionId}", sessionId)
                .cookie(owner.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId").value(sessionId));
    mockMvc
        .perform(
            get("/api/v2/precheck-sessions/{sessionId}/runs", sessionId)
                .cookie(owner.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].sequence").value(1));

    var admin = login("mock-admin");
    mockMvc
        .perform(
            get("/api/v2/precheck-sessions/{sessionId}", sessionId)
                .cookie(admin.getResponse().getCookie("SESSION")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void completenessPoliciesAreReadOnlyAndHistoricalRunKeepsItsSnapshot() throws Exception {
    var owner = login("mock-precheck-tdh");
    var created = createSession(owner, "mock-host-policy-001");
    var createdJson = objectMapper.readTree(created.getResponse().getContentAsByteArray());
    var sessionId = createdJson.path("sessionId").asText();
    var originalMissing = createdJson.at("/latestRun/result/completeness/missingFieldCodes");

    mockMvc
        .perform(
            get("/api/v2/completeness-policies").cookie(owner.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(4))
        .andExpect(jsonPath("$.items[0].policyVersion").value("mock-completeness-v1"));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE completeness_policy SET issue_specific_field_codes = 'NEW_REQUIRED_FIELD' WHERE policy_version = 'mock-completeness-v1' AND issue_type_code = 'FUNCTIONAL_FAILURE'"))
        .hasRootCauseInstanceOf(SQLException.class)
        .hasMessageContaining("immutable");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM completeness_policy WHERE policy_version = 'mock-completeness-v1' AND issue_type_code = 'FUNCTIONAL_FAILURE'"))
        .hasRootCauseInstanceOf(SQLException.class)
        .hasMessageContaining("immutable");

    var restored =
        mockMvc
            .perform(
                get("/api/v2/precheck-sessions/{sessionId}", sessionId)
                    .cookie(owner.getResponse().getCookie("SESSION")))
            .andExpect(status().isOk())
            .andReturn();
    var restoredMissing =
        objectMapper
            .readTree(restored.getResponse().getContentAsByteArray())
            .at("/latestRun/result/completeness/missingFieldCodes");
    org.assertj.core.api.Assertions.assertThat(restoredMissing).isEqualTo(originalMissing);
  }

  @Test
  void storedRunSnapshotsRejectUpdateAndDelete() throws Exception {
    var owner = login("mock-precheck-tdh");
    var created = createSession(owner, "mock-host-immutable-001");
    var runId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .at("/latestRun/runId")
            .asText();

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE precheck_run_v2 SET status = 'COMPLETED' WHERE run_id = ?",
                    java.util.UUID.fromString(runId)))
        .hasRootCauseInstanceOf(SQLException.class)
        .hasMessageContaining("immutable");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM precheck_run_v2 WHERE run_id = ?",
                    java.util.UUID.fromString(runId)))
        .hasRootCauseInstanceOf(SQLException.class)
        .hasMessageContaining("immutable");
  }

  @Test
  void businessKeyReplaysNormalizedContextAndRejectsDifferentContext() throws Exception {
    var login = login("mock-precheck-tdh");
    var first = createSession(login, "mock-host-business-idempotency-001");
    var sessionId =
        objectMapper
            .readTree(first.getResponse().getContentAsByteArray())
            .path("sessionId")
            .asText();
    mockMvc
        .perform(
            post("/api/v2/precheck-sessions")
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    sessionRequest("mock-host-business-idempotency-001", "模拟数据：初始标题")
                        .replace("TDH（模拟数据）", "显示名变化但编码不变")))
        .andExpect(status().isOk())
        .andExpect(header().string("Idempotency-Replayed", "true"))
        .andExpect(jsonPath("$.sessionId").value(sessionId));
    mockMvc
        .perform(
            post("/api/v2/precheck-sessions")
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionRequest("mock-host-business-idempotency-001", "模拟数据：不同标题")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONTEXT_CONFLICT"));
  }

  @Test
  void idempotencyReplayNeverExposesAnotherPrecheckUsersSession() throws Exception {
    jdbc.update(
        "INSERT INTO local_identity(user_code, display_name, seed_version, enabled, mock_data) VALUES ('mock-precheck-tdh-two', 'TDH 预诊用户二（模拟数据）', 'local-identity-v1', TRUE, TRUE)");
    jdbc.update(
        "INSERT INTO local_identity_role(user_code, role_code) VALUES ('mock-precheck-tdh-two', 'PRECHECK_USER')");
    jdbc.update(
        "INSERT INTO local_identity_product_line(user_code, product_line_code) VALUES ('mock-precheck-tdh-two', 'TDH')");
    try {
      var owner = login("mock-precheck-tdh");
      var created = createSession(owner, "mock-host-cross-owner-001");
      var sessionId =
          objectMapper
              .readTree(created.getResponse().getContentAsByteArray())
              .path("sessionId")
              .asText();
      createRun(
              owner,
              sessionId,
              "mock-host-cross-owner-001",
              "cross-owner-run-command-001",
              "模拟数据：第二轮",
              "SQL_ENGINE")
          .andExpect(status().isCreated());

      var other = login("mock-precheck-tdh-two");
      mockMvc
          .perform(
              post("/api/v2/precheck-sessions")
                  .cookie(other.getResponse().getCookie("SESSION"))
                  .header("X-CSRF-Token", other.getResponse().getHeader("X-CSRF-Token"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(sessionRequest("mock-host-cross-owner-001", "模拟数据：初始标题")))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONTEXT_CONFLICT"));
      createRun(
              other,
              sessionId,
              "mock-host-cross-owner-001",
              "cross-owner-run-command-001",
              "模拟数据：第二轮",
              "SQL_ENGINE")
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    } finally {
      jdbc.update(
          "DELETE FROM local_identity_product_line WHERE user_code = 'mock-precheck-tdh-two'");
      jdbc.update("DELETE FROM local_identity_role WHERE user_code = 'mock-precheck-tdh-two'");
      jdbc.update("DELETE FROM auth_session WHERE user_code = 'mock-precheck-tdh-two'");
      jdbc.update("DELETE FROM local_identity WHERE user_code = 'mock-precheck-tdh-two'");
    }
  }

  @Test
  void selfServiceTerminationRequiresExplicitConfirmation() throws Exception {
    var login = login("mock-precheck-tdh");
    var created = createSession(login, "mock-host-explicit-confirmation-001");
    var sessionId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("sessionId")
            .asText();

    mockMvc
        .perform(
            post("/api/v2/precheck-sessions/{sessionId}/self-service-confirmations", sessionId)
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "explicit-confirmation-command-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmed\":false,\"reason\":\"模拟数据：尚未确认\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void sessionListingHonorsStatusAndSortContract() throws Exception {
    var login = login("mock-precheck-tdh");
    var active = createSession(login, "mock-host-list-active-001");
    var terminated = createSession(login, "mock-host-list-terminated-001");
    var terminatedId =
        objectMapper
            .readTree(terminated.getResponse().getContentAsByteArray())
            .path("sessionId")
            .asText();
    mockMvc
        .perform(
            post("/api/v2/precheck-sessions/{sessionId}/self-service-confirmations", terminatedId)
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "list-termination-command-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmed\":true,\"reason\":\"模拟数据：人工明确确认\"}"))
        .andExpect(status().isCreated());
    var activeId =
        objectMapper
            .readTree(active.getResponse().getContentAsByteArray())
            .path("sessionId")
            .asText();

    mockMvc
        .perform(
            get("/api/v2/precheck-sessions")
                .cookie(login.getResponse().getCookie("SESSION"))
                .queryParam("status", "ACTIVE")
                .queryParam("sortBy", "createdAt")
                .queryParam("sortDirection", "ASC"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.items[*].status")
                .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ACTIVE"))))
        .andExpect(jsonPath("$.items[*].sessionId").value(org.hamcrest.Matchers.hasItem(activeId)))
        .andExpect(
            jsonPath("$.items[*].sessionId")
                .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(terminatedId))))
        .andExpect(jsonPath("$.page.sortBy").value("createdAt"))
        .andExpect(jsonPath("$.page.sortDirection").value("ASC"));
  }

  @Test
  void completenessUsesThePersistedPolicyFieldLists() throws Exception {
    var login = login("mock-precheck-tdh");
    var completeContext =
        sessionRequest("mock-host-policy-fields-001", "模拟数据：策略字段")
            .replace(
                "\"additionalInformation\":[]",
                "\"additionalInformation\":["
                    + "{\"fieldCode\":\"OCCURRED_AT\",\"displayName\":\"发生时间（模拟数据）\",\"value\":\"2026-07-16T08:00:00Z\"},"
                    + "{\"fieldCode\":\"ERROR_MESSAGE\",\"displayName\":\"错误信息（模拟数据）\",\"value\":\"MOCK-1001\"},"
                    + "{\"fieldCode\":\"REPRODUCTION_STEPS\",\"displayName\":\"复现步骤（模拟数据）\",\"value\":\"模拟步骤\"},"
                    + "{\"fieldCode\":\"RECENT_CHANGES\",\"displayName\":\"近期变更（模拟数据）\",\"value\":\"无\"}]");

    mockMvc
        .perform(
            post("/api/v2/precheck-sessions")
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(completeContext))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.latestRun.status").value("DEGRADED"))
        .andExpect(jsonPath("$.latestRun.result.completeness.complete").value(true))
        .andExpect(jsonPath("$.latestRun.result.completeness.missingFieldCodes").isEmpty());
  }

  @Test
  void unknownIssueTypeIsRejectedAsContractValidationError() throws Exception {
    var login = login("mock-precheck-tdh");

    mockMvc
        .perform(
            post("/api/v2/precheck-sessions")
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    sessionRequest("mock-host-unknown-issue-001", "模拟数据：未知问题类型")
                        .replace("FUNCTIONAL_FAILURE", "UNKNOWN_ISSUE")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.safeDetails.field").value("context.issueType.code"));

    mockMvc
        .perform(
            get("/api/v2/completeness-policies")
                .cookie(login.getResponse().getCookie("SESSION"))
                .queryParam("issueTypeCode", "UNKNOWN_ISSUE"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.safeDetails.field").value("issueTypeCode"));
  }

  @Test
  void productLineScopeDenialUsesTheContractErrorCode() throws Exception {
    var login = login("mock-precheck-tdh");

    mockMvc
        .perform(
            post("/api/v2/precheck-sessions")
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    sessionRequest("mock-host-product-line-denied-001", "模拟数据：越权产品线")
                        .replace("\"code\":\"TDH\"", "\"code\":\"STREAMING\"")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INSUFFICIENT_ROLE"));
  }

  @Test
  void componentChangeRequiresNewSessionAndFourthRunKeepsContinueSubmission() throws Exception {
    var login = login("mock-precheck-tdh");
    var created = createSession(login, "mock-host-limit-001");
    var sessionId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("sessionId")
            .asText();
    createRun(
            login,
            sessionId,
            "mock-host-limit-001",
            "new-session-required-001",
            "模拟数据：组件变化",
            "BROKER")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("NEW_SESSION_REQUIRED"));
    createRun(
            login,
            sessionId,
            "mock-host-limit-001",
            "run-limit-command-002",
            "模拟数据：第二轮",
            "SQL_ENGINE")
        .andExpect(status().isCreated());
    createRun(
            login,
            sessionId,
            "mock-host-limit-001",
            "run-limit-command-003",
            "模拟数据：第三轮",
            "SQL_ENGINE")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sequence").value(3))
        .andExpect(
            jsonPath("$.result.allowedActions")
                .value(org.hamcrest.Matchers.hasItem("CONTINUE_SUBMISSION")));
    createRun(
            login,
            sessionId,
            "mock-host-limit-001",
            "run-limit-command-004",
            "模拟数据：第四轮",
            "SQL_ENGINE")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("RUN_LIMIT_REACHED"))
        .andExpect(
            jsonPath("$.safeDetails.allowedActions")
                .value(org.hamcrest.Matchers.hasItem("CONTINUE_SUBMISSION")));
  }

  @Test
  void concurrentBusinessKeyCreationProducesOneSession() throws Exception {
    var login = login("mock-precheck-tdh");
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var first = executor.submit(() -> concurrentCreate(login, ready, start));
      var second = executor.submit(() -> concurrentCreate(login, ready, start));
      org.assertj.core.api.Assertions.assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      org.assertj.core.api.Assertions.assertThat(List.of(first.get(), second.get()))
          .containsExactlyInAnyOrder(201, 200);
      org.assertj.core.api.Assertions.assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM precheck_session_v2 WHERE host_request_id = 'mock-host-concurrent-001'",
                  Integer.class))
          .isEqualTo(1);
    } finally {
      executor.shutdownNow();
    }
  }

  private int concurrentCreate(MvcResult login, CountDownLatch ready, CountDownLatch start)
      throws Exception {
    ready.countDown();
    if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Concurrent start timed out");
    return mockMvc
        .perform(
            post("/api/v2/precheck-sessions")
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionRequest("mock-host-concurrent-001", "模拟数据：并发创建")))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private org.springframework.test.web.servlet.ResultActions createRun(
      MvcResult login,
      String sessionId,
      String hostRequestId,
      String key,
      String title,
      String component)
      throws Exception {
    return mockMvc.perform(
        post("/api/v2/precheck-sessions/{sessionId}/runs", sessionId)
            .cookie(login.getResponse().getCookie("SESSION"))
            .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(sessionRequest(hostRequestId, title).replace("SQL_ENGINE", component)));
  }

  private MvcResult createSession(MvcResult login, String hostRequestId) throws Exception {
    return mockMvc
        .perform(
            post("/api/v2/precheck-sessions")
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionRequest(hostRequestId, "模拟数据：初始标题")))
        .andExpect(status().isCreated())
        .andReturn();
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

  private String sessionRequest(String hostRequestId, String title) {
    return """
        {"context":{"sourceSystem":"SANDBOX","hostRequestId":"%s","formSchemaVersion":"1.0","issueType":{"code":"FUNCTIONAL_FAILURE","displayName":"功能故障（模拟数据）"},"productLine":{"code":"TDH","displayName":"TDH（模拟数据）"},"product":{"code":"INCEPTOR","displayName":"Inceptor（模拟数据）"},"component":{"code":"SQL_ENGINE","displayName":"SQL 引擎（模拟数据）"},"version":"9.1.0-mock","severity":{"code":"S2","displayName":"S2（模拟数据）"},"serviceType":{"code":"CONSULTATION","displayName":"咨询（模拟数据）"},"title":"%s","descriptionPlainText":"模拟数据：执行脱敏 SQL 时返回错误码 MOCK-1001。","additionalInformation":[],"impactScope":"模拟数据：单个测试任务","attachments":[]}}
        """
        .formatted(hostRequestId, title);
  }
}
