package com.transwarp.serviceinsight.admin.reset.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:admin_reset_non_local_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "app.environment-code=TEST"
    })
@AutoConfigureMockMvc
class AdminResetNonLocalControllerTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void resetIsUnavailableOutsideTheExplicitLocalEnvironment() throws Exception {
    var login =
        mockMvc
            .perform(
                post("/api/v2/auth-sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userCode\":\"mock-admin\",\"mockData\":true}"))
            .andExpect(status().isCreated())
            .andReturn();

    mockMvc
        .perform(
            post("/api/v2/admin/resets")
                .cookie(login.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", login.getResponse().getHeader("X-CSRF-Token"))
                .header("Idempotency-Key", "admin-reset-non-local-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"environmentCode":"LOCAL","confirmationPhrase":"RESET LOCAL MOCK DATA","reason":"模拟数据：非本地环境拒绝"}
                    """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("ADMIN_RESET_NOT_AVAILABLE"));

    mockMvc
        .perform(get("/api/v2/admin/resets").cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("ADMIN_RESET_NOT_AVAILABLE"));

    mockMvc
        .perform(
            get("/api/v2/admin/resets/{resetId}", java.util.UUID.randomUUID())
                .cookie(login.getResponse().getCookie("SESSION")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("ADMIN_RESET_NOT_AVAILABLE"));
  }
}
