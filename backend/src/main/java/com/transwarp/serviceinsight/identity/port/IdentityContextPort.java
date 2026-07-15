package com.transwarp.serviceinsight.identity.port;

import com.transwarp.serviceinsight.identity.domain.AuthSession;
import com.transwarp.serviceinsight.identity.domain.IdentityContext;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdentityContextPort {
  Optional<IdentityContext> findEnabledIdentity(String userCode);

  Optional<AuthSession> findActiveSession(UUID sessionId, Instant now);

  void replaceSession(UUID previousSessionId, AuthSession newSession, Instant replacedAt);

  boolean invalidateSession(UUID sessionId, String csrfToken, Instant invalidatedAt);
}
