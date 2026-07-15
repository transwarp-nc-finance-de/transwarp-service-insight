package com.transwarp.serviceinsight.identity.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = AuthSessionControllerTest.TestClockConfiguration.class)
class AuthSessionControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private MutableClock clock;

  @BeforeEach
  void resetClock() {
    clock.reset();
  }

  @Test
  void createsServerSessionForConfirmedMockIdentity() throws Exception {
    mockMvc
        .perform(
            post("/api/v2/auth-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userCode\":\"mock-precheck-tdh\",\"mockData\":true}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(header().string("X-CSRF-Token", matchesPattern("[A-Za-z0-9_-]{43}")))
        .andExpect(
            header()
                .string(
                    "Set-Cookie", matchesPattern("SESSION=[^;]+; Path=/; HttpOnly; SameSite=Lax")))
        .andExpect(jsonPath("$.userCode").value("mock-precheck-tdh"))
        .andExpect(jsonPath("$.displayName").value("TDH 预诊用户（模拟数据）"))
        .andExpect(jsonPath("$.roles", hasSize(1)))
        .andExpect(jsonPath("$.roles[0]").value("PRECHECK_USER"))
        .andExpect(jsonPath("$.productLineCodes", hasSize(1)))
        .andExpect(jsonPath("$.productLineCodes[0]").value("TDH"))
        .andExpect(jsonPath("$.mockData").value(true));
  }

  @Test
  void restoresCurrentSessionWithoutRotatingCsrfToken() throws Exception {
    var login = login("mock-knowledge-editor");
    var sessionCookie = login.getResponse().getCookie("SESSION");
    var csrfToken = login.getResponse().getHeader("X-CSRF-Token");

    mockMvc
        .perform(get("/api/v2/auth-sessions/current").cookie(sessionCookie))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(header().string("X-CSRF-Token", csrfToken))
        .andExpect(jsonPath("$.userCode").value("mock-knowledge-editor"))
        .andExpect(jsonPath("$.roles[0]").value("KNOWLEDGE_EDITOR"))
        .andExpect(jsonPath("$.productLineCodes", hasSize(2)))
        .andExpect(jsonPath("$.productLineCodes[0]").value("STREAMING"))
        .andExpect(jsonPath("$.productLineCodes[1]").value("TDH"));
  }

  @Test
  void rejectsMissingOrUnknownSessionWithV2UnauthenticatedError() throws Exception {
    mockMvc
        .perform(get("/api/v2/auth-sessions/current"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(jsonPath("$.retryable").value(false))
        .andExpect(jsonPath("$.fieldErrors").isArray())
        .andExpect(jsonPath("$.requestId").isNotEmpty())
        .andExpect(jsonPath("$.safeDetails.mockData").value(true));

    mockMvc
        .perform(
            post("/api/v2/auth-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userCode\":\"unknown-user\",\"mockData\":true}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void distinguishesCsrfFailureAndInvalidatesSessionOnLogout() throws Exception {
    var login = login("mock-admin");
    var sessionCookie = login.getResponse().getCookie("SESSION");
    var csrfToken = login.getResponse().getHeader("X-CSRF-Token");

    mockMvc
        .perform(delete("/api/v2/auth-sessions/current").cookie(sessionCookie))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"))
        .andExpect(jsonPath("$.retryable").value(false))
        .andExpect(jsonPath("$.safeDetails.csrfToken").doesNotExist());

    mockMvc
        .perform(
            delete("/api/v2/auth-sessions/current")
                .cookie(sessionCookie)
                .header("X-CSRF-Token", csrfToken))
        .andExpect(status().isNoContent())
        .andExpect(header().string("Set-Cookie", matchesPattern("SESSION=;.*Max-Age=0.*")));

    mockMvc
        .perform(get("/api/v2/auth-sessions/current").cookie(sessionCookie))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    mockMvc
        .perform(
            delete("/api/v2/auth-sessions/current")
                .cookie(sessionCookie)
                .header("X-CSRF-Token", csrfToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void rejectsCsrfTokenFromAnotherSession() throws Exception {
    var first = login("mock-knowledge-editor");
    var second = login("mock-knowledge-reviewer");

    mockMvc
        .perform(
            delete("/api/v2/auth-sessions/current")
                .cookie(first.getResponse().getCookie("SESSION"))
                .header("X-CSRF-Token", second.getResponse().getHeader("X-CSRF-Token")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"));
  }

  @Test
  void switchingIdentityReplacesSessionAndCsrfToken() throws Exception {
    var first = login("mock-precheck-tdh");
    var firstCookie = first.getResponse().getCookie("SESSION");
    var firstToken = first.getResponse().getHeader("X-CSRF-Token");

    var switched =
        mockMvc
            .perform(
                post("/api/v2/auth-sessions")
                    .cookie(firstCookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userCode\":\"mock-knowledge-reviewer\",\"mockData\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.roles[0]").value("KNOWLEDGE_REVIEWER"))
            .andReturn();

    assertNotEquals(firstCookie.getValue(), switched.getResponse().getCookie("SESSION").getValue());
    assertNotEquals(firstToken, switched.getResponse().getHeader("X-CSRF-Token"));
    mockMvc
        .perform(get("/api/v2/auth-sessions/current").cookie(firstCookie))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsNonMockLoginRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/v2/auth-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userCode\":\"mock-admin\",\"mockData\":false}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("mockData"))
        .andExpect(jsonPath("$.retryable").value(false));
  }

  @ParameterizedTest
  @CsvSource({
    "mock-precheck-tdh,PRECHECK_USER,TDH,1",
    "mock-knowledge-editor,KNOWLEDGE_EDITOR,STREAMING,2",
    "mock-knowledge-reviewer,KNOWLEDGE_REVIEWER,STREAMING,2",
    "mock-admin,ADMIN,STREAMING,2"
  })
  void exposesConfirmedRoleAndProductLineMatrix(
      String userCode, String role, String firstProductLine, int productLineCount)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v2/auth-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userCode\":\"" + userCode + "\",\"mockData\":true}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.roles", hasSize(1)))
        .andExpect(jsonPath("$.roles[0]").value(role))
        .andExpect(jsonPath("$.productLineCodes", hasSize(productLineCount)))
        .andExpect(jsonPath("$.productLineCodes[0]").value(firstProductLine));
  }

  @Test
  void expiresSessionAndItsCsrfTokenTogether() throws Exception {
    var login = login("mock-precheck-tdh");
    var sessionCookie = login.getResponse().getCookie("SESSION");
    var csrfToken = login.getResponse().getHeader("X-CSRF-Token");

    clock.advance(Duration.ofHours(8).plusSeconds(1));

    mockMvc
        .perform(get("/api/v2/auth-sessions/current").cookie(sessionCookie))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    mockMvc
        .perform(
            delete("/api/v2/auth-sessions/current")
                .cookie(sessionCookie)
                .header("X-CSRF-Token", csrfToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
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

  @TestConfiguration
  static class TestClockConfiguration {
    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock();
    }
  }

  static final class MutableClock extends Clock {
    private static final Instant INITIAL = Instant.parse("2026-07-15T00:00:00Z");
    private Instant current = INITIAL;

    void reset() {
      current = INITIAL;
    }

    void advance(Duration duration) {
      current = current.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return current;
    }
  }
}
