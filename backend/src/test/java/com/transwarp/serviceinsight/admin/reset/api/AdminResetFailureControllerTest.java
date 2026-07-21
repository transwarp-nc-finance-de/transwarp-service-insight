package com.transwarp.serviceinsight.admin.reset.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.admin.reset.port.LocalResetFileStore;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:admin_reset_failure_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "app.environment-code=LOCAL"
    })
@AutoConfigureMockMvc
@ContextConfiguration(classes = AdminResetFailureControllerTest.FailingStorageConfiguration.class)
class AdminResetFailureControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void storageFailurePersistsAFailedTaskAndSafeAudit() throws Exception {
    var login =
        mockMvc
            .perform(
                post("/api/v2/auth-sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userCode\":\"mock-admin\",\"mockData\":true}"))
            .andExpect(status().isCreated())
            .andReturn();
    var marker = "模拟数据：不得复制到审计的失败原因";
    var created =
        mockMvc
            .perform(
                post("/api/v2/admin/resets")
                    .cookie(login.getResponse().getCookie("SESSION"))
                    .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                    .header("Idempotency-Key", "admin-reset-failure-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"environmentCode\":\"LOCAL\",\"confirmationPhrase\":\"RESET LOCAL MOCK DATA\",\"reason\":\""
                            + marker
                            + "\"}"))
            .andExpect(status().isAccepted())
            .andReturn();
    var taskId =
        UUID.fromString(
            objectMapper
                .readTree(created.getResponse().getContentAsByteArray())
                .path("taskId")
                .asText());

    for (int attempt = 0; attempt < 100; attempt++) {
      var current =
          jdbc.queryForMap("SELECT status,error_code FROM admin_reset WHERE task_id=?", taskId);
      if ("FAILED".equals(current.get("status"))) {
        assertTrue("RESET_FAILED".equals(current.get("error_code")));
        var metadata =
            jdbc.queryForObject(
                "SELECT metadata FROM audit_event_v2 WHERE subject_id=? AND action='ADMIN_RESET_FAILED'",
                String.class,
                taskId);
        assertFalse(metadata.contains(marker));
        return;
      }
      Thread.sleep(20);
    }
    throw new AssertionError("failed reset did not reach a terminal state");
  }

  @TestConfiguration
  static class FailingStorageConfiguration {
    @Bean
    @Primary
    LocalResetFileStore failingLocalResetFileStore() {
      return new LocalResetFileStore() {
        @Override
        public void clearOriginalFiles() throws IOException {
          throw new IOException("simulated local storage failure");
        }
      };
    }
  }
}
