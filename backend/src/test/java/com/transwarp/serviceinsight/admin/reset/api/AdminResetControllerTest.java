package com.transwarp.serviceinsight.admin.reset.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.knowledge.ingestion.port.OriginalFileStorage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:admin_reset_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "app.environment-code=LOCAL",
      "app.knowledge.storage-path=${java.io.tmpdir}/service-insight-admin-reset-test"
    })
@AutoConfigureMockMvc
class AdminResetControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private OriginalFileStorage fileStorage;

  @Test
  void adminCreatesAndReadsAnAsynchronousLocalReset() throws Exception {
    var login = login("mock-admin");

    var created =
        mockMvc
            .perform(
                post("/api/v2/admin/resets")
                    .cookie(login.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "admin-reset-command-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"environmentCode":"LOCAL","confirmationPhrase":"RESET LOCAL MOCK DATA","reason":"模拟数据：恢复版本化初始状态"}
                        """))
            .andExpect(status().isAccepted())
            .andExpect(header().string("Idempotency-Replayed", "false"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.attempt").value(0))
            .andExpect(jsonPath("$.maxAttempts").value(3))
            .andExpect(jsonPath("$.environmentCode").value("LOCAL"))
            .andExpect(jsonPath("$.confirmedBy").value("mock-admin"))
            .andExpect(jsonPath("$.auditEventId").isNotEmpty())
            .andExpect(jsonPath("$.mockData").value(true))
            .andReturn();

    var resetId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("taskId")
            .asText();
    awaitTerminal(resetId);
    var newLogin = login("mock-admin");
    mockMvc
        .perform(
            get("/api/v2/admin/resets/{resetId}", resetId)
                .cookie(newLogin.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.taskId").value(resetId))
        .andExpect(jsonPath("$.status").value("SUCCEEDED"));
  }

  @Test
  void resetRequiresAdminCsrfAndTheExactConfirmationPhrase() throws Exception {
    var user = login("mock-precheck-tdh");
    mockMvc
        .perform(
            resetRequest(user, "admin-reset-security-001", "RESET LOCAL MOCK DATA", "模拟数据：拒绝非管理员"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INSUFFICIENT_ROLE"));

    var admin = login("mock-admin");
    mockMvc
        .perform(
            post("/api/v2/admin/resets")
                .cookie(admin.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", "invalid-csrf-token")
                .header("Idempotency-Key", "admin-reset-security-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"environmentCode":"LOCAL","confirmationPhrase":"RESET LOCAL MOCK DATA","reason":"模拟数据：拒绝错误 CSRF"}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"));

    mockMvc
        .perform(resetRequest(admin, "admin-reset-security-003", "reset", "模拟数据：拒绝错误确认短语"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void resetHasAnIndependentIdempotencyBoundaryAndListsHistory() throws Exception {
    var admin = login("mock-admin");
    var created =
        mockMvc
            .perform(
                resetRequest(admin, "admin-reset-replay-001", "RESET LOCAL MOCK DATA", "模拟数据：幂等重放"))
            .andExpect(status().isAccepted())
            .andExpect(header().string("Idempotency-Replayed", "false"))
            .andReturn();
    var taskId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("taskId")
            .asText();
    awaitTerminal(taskId);
    admin = login("mock-admin");
    mockMvc
        .perform(
            resetRequest(admin, "admin-reset-replay-001", "RESET LOCAL MOCK DATA", "模拟数据：幂等重放"))
        .andExpect(status().isOk())
        .andExpect(header().string("Idempotency-Replayed", "true"))
        .andExpect(jsonPath("$.taskId").value(taskId));

    mockMvc
        .perform(
            resetRequest(admin, "admin-reset-replay-001", "RESET LOCAL MOCK DATA", "模拟数据：不同请求"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));

    mockMvc
        .perform(get("/api/v2/admin/resets").cookie(admin.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].taskId").isNotEmpty())
        .andExpect(jsonPath("$.page.totalItems").isNumber());
  }

  @Test
  void resetClearsBusinessStateAndRebuildsVersionedLocalSeeds() throws Exception {
    var oldFileId = UUID.randomUUID();
    var oldContent = "模拟数据：重置前原始知识正文".getBytes(StandardCharsets.UTF_8);
    var oldStorageKey = fileStorage.store(oldFileId, "old.txt", oldContent);
    assertArrayEquals(oldContent, fileStorage.read(oldStorageKey));
    var admin = login("mock-admin");

    var created =
        mockMvc
            .perform(
                resetRequest(
                    admin, "admin-reset-execution-001", "RESET LOCAL MOCK DATA", "模拟数据：验证完整重置"))
            .andExpect(status().isAccepted())
            .andReturn();
    var taskId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("taskId")
            .asText();

    awaitTerminal(taskId);
    assertThrows(IOException.class, () -> fileStorage.read(oldStorageKey));
    org.junit.jupiter.api.Assertions.assertEquals(
        0, jdbc.queryForObject("SELECT COUNT(*) FROM auth_session", Integer.class));
    org.junit.jupiter.api.Assertions.assertEquals(
        4, jdbc.queryForObject("SELECT COUNT(*) FROM local_identity", Integer.class));
    org.junit.jupiter.api.Assertions.assertEquals(
        4, jdbc.queryForObject("SELECT COUNT(*) FROM completeness_policy", Integer.class));
    org.junit.jupiter.api.Assertions.assertTrue(
        jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_chunk_index", Integer.class) > 0);
    org.junit.jupiter.api.Assertions.assertEquals(
        0, jdbc.queryForObject("SELECT COUNT(*) FROM evaluation_run", Integer.class));
    org.junit.jupiter.api.Assertions.assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit_event_v2 WHERE subject_id=? AND action='ADMIN_RESET_COMPLETED' AND outcome='SUCCEEDED'",
            Integer.class,
            UUID.fromString(taskId)));

    var newLogin = login("mock-admin");
    mockMvc
        .perform(
            get("/api/v2/admin/resets/{resetId}", taskId)
                .cookie(newLogin.getResponse().getCookie("SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCEEDED"))
        .andExpect(jsonPath("$.attempt").value(1));
  }

  private void awaitTerminal(String taskId) throws InterruptedException {
    for (int attempt = 0; attempt < 200; attempt++) {
      var status =
          jdbc.queryForObject(
              "SELECT status FROM admin_reset WHERE task_id=?",
              String.class,
              UUID.fromString(taskId));
      if ("SUCCEEDED".equals(status)) return;
      if ("FAILED".equals(status)) throw new AssertionError("admin reset failed");
      Thread.sleep(20);
    }
    throw new AssertionError("admin reset did not reach a terminal state");
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder resetRequest(
      MvcResult login, String idempotencyKey, String confirmationPhrase, String reason) {
    return post("/api/v2/admin/resets")
        .cookie(login.getResponse().getCookie("SESSION"))
        .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
        .header("Idempotency-Key", idempotencyKey)
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            "{\"environmentCode\":\"LOCAL\",\"confirmationPhrase\":\""
                + confirmationPhrase
                + "\",\"reason\":\""
                + reason
                + "\"}");
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
