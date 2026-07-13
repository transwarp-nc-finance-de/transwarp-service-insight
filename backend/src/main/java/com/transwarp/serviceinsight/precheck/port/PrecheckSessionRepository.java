package com.transwarp.serviceinsight.precheck.port;

import com.transwarp.serviceinsight.precheck.domain.PrecheckSession;
import java.util.Optional;
import java.util.UUID;

public interface PrecheckSessionRepository {
  void save(PrecheckSession session);

  Optional<PrecheckSession> findById(UUID sessionId);

  Optional<PrecheckSession> findByPrecheckId(UUID precheckId);
}
