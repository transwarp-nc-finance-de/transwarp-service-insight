package com.transwarp.serviceinsight.identity.api;

import com.transwarp.serviceinsight.identity.domain.AuthSession;
import com.transwarp.serviceinsight.identity.domain.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthSessionResponse(
    UUID sessionId,
    String userCode,
    String displayName,
    List<Role> roles,
    List<String> productLineCodes,
    Instant expiresAt,
    boolean mockData) {

  static AuthSessionResponse from(AuthSession session) {
    var identity = session.identity();
    return new AuthSessionResponse(
        session.sessionId(),
        identity.userCode(),
        identity.displayName(),
        identity.roles(),
        identity.productLineCodes(),
        session.expiresAt(),
        identity.mockData());
  }
}
