package com.transwarp.serviceinsight.identity.api;

import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.identity.domain.AuthSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/auth-sessions")
public class AuthSessionController {
  static final String SESSION_COOKIE = "SESSION";
  static final String CSRF_HEADER = "X-CSRF-Token";

  private final AuthSessionApplicationService authSessions;

  public AuthSessionController(AuthSessionApplicationService authSessions) {
    this.authSessions = authSessions;
  }

  @PostMapping
  ResponseEntity<AuthSessionResponse> create(
      @Valid @RequestBody CreateAuthSessionRequest request,
      @CookieValue(name = SESSION_COOKIE, required = false) String previousSessionCookie) {
    var session = authSessions.create(request.userCode(), previousSessionCookie);
    return withSessionHeaders(session, HttpStatus.CREATED);
  }

  @GetMapping("/current")
  ResponseEntity<AuthSessionResponse> current(
      @CookieValue(name = SESSION_COOKIE, required = false) String sessionCookie) {
    return withSessionHeaders(authSessions.current(sessionCookie), HttpStatus.OK);
  }

  @DeleteMapping("/current")
  ResponseEntity<Void> logout(
      @CookieValue(name = SESSION_COOKIE, required = false) String sessionCookie,
      @RequestHeader(name = CSRF_HEADER, required = false) String csrfToken) {
    authSessions.logout(sessionCookie, csrfToken);
    var expiredCookie =
        ResponseCookie.from(SESSION_COOKIE, "")
            .httpOnly(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build();
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
        .build();
  }

  private ResponseEntity<AuthSessionResponse> withSessionHeaders(
      AuthSession session, HttpStatus status) {
    var cookie =
        ResponseCookie.from(SESSION_COOKIE, session.sessionId().toString())
            .httpOnly(true)
            .sameSite("Lax")
            .path("/")
            .build();
    return ResponseEntity.status(status)
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .header(CSRF_HEADER, session.csrfToken())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(AuthSessionResponse.from(session));
  }
}
