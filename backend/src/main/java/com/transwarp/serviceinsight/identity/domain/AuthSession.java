package com.transwarp.serviceinsight.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthSession(
    UUID sessionId,
    IdentityContext identity,
    String csrfToken,
    Instant createdAt,
    Instant expiresAt) {

  public AuthSession {
    Objects.requireNonNull(sessionId);
    Objects.requireNonNull(identity);
    Objects.requireNonNull(csrfToken);
    Objects.requireNonNull(createdAt);
    Objects.requireNonNull(expiresAt);
    if (!expiresAt.isAfter(createdAt)) {
      throw new IllegalArgumentException("Session expiry must be after creation");
    }
  }
}
