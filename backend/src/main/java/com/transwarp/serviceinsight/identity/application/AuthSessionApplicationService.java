package com.transwarp.serviceinsight.identity.application;

import com.transwarp.serviceinsight.identity.domain.AuthSession;
import com.transwarp.serviceinsight.identity.port.IdentityContextPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionApplicationService {
  private static final int CSRF_TOKEN_BYTES = 32;

  private final IdentityContextPort identityContextPort;
  private final Clock clock;
  private final Duration sessionTtl;
  private final SecureRandom secureRandom = new SecureRandom();

  public AuthSessionApplicationService(
      IdentityContextPort identityContextPort,
      Clock clock,
      @Value("${app.identity.session-ttl:PT8H}") Duration sessionTtl) {
    this.identityContextPort = identityContextPort;
    this.clock = clock;
    this.sessionTtl = sessionTtl;
  }

  public AuthSession create(String userCode, String previousSessionCookie) {
    var identity =
        identityContextPort
            .findEnabledIdentity(userCode)
            .orElseThrow(UnauthenticatedException::new);
    var now = clock.instant();
    var session =
        new AuthSession(
            UUID.randomUUID(), identity, generateCsrfToken(), now, now.plus(sessionTtl));
    identityContextPort.replaceSession(parseSessionId(previousSessionCookie), session, now);
    return session;
  }

  public AuthSession current(String sessionCookie) {
    var sessionId = parseRequiredSessionId(sessionCookie);
    return identityContextPort
        .findActiveSession(sessionId, clock.instant())
        .orElseThrow(UnauthenticatedException::new);
  }

  public void logout(String sessionCookie, String csrfToken) {
    var session = current(sessionCookie);
    if (!csrfMatches(session.csrfToken(), csrfToken)) {
      throw new CsrfValidationFailedException();
    }
    if (!identityContextPort.invalidateSession(
        session.sessionId(), session.csrfToken(), clock.instant())) {
      throw new UnauthenticatedException();
    }
  }

  public AuthSession requireWriteSession(String sessionCookie, String csrfToken) {
    var session = current(sessionCookie);
    if (!csrfMatches(session.csrfToken(), csrfToken)) {
      throw new CsrfValidationFailedException();
    }
    return session;
  }

  private String generateCsrfToken() {
    var bytes = new byte[CSRF_TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private boolean csrfMatches(String expected, String actual) {
    if (actual == null || actual.isBlank()) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }

  private UUID parseRequiredSessionId(String value) {
    var sessionId = parseSessionId(value);
    if (sessionId == null) {
      throw new UnauthenticatedException();
    }
    return sessionId;
  }

  private UUID parseSessionId(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }
}
