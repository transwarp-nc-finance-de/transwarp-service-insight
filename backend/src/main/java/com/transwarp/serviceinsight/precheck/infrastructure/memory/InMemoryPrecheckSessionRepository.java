package com.transwarp.serviceinsight.precheck.infrastructure.memory;

import com.transwarp.serviceinsight.precheck.domain.PrecheckSession;
import com.transwarp.serviceinsight.precheck.port.PrecheckSessionRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPrecheckSessionRepository implements PrecheckSessionRepository {
  private final ConcurrentHashMap<UUID, PrecheckSession> sessions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, UUID> precheckIndex = new ConcurrentHashMap<>();

  @Override
  public void save(PrecheckSession session) {
    sessions.put(session.id(), session);
    precheckIndex.put(session.precheckId(), session.id());
  }

  @Override
  public Optional<PrecheckSession> findById(UUID id) {
    return Optional.ofNullable(sessions.get(id));
  }

  @Override
  public Optional<PrecheckSession> findByPrecheckId(UUID id) {
    return Optional.ofNullable(precheckIndex.get(id)).flatMap(this::findById);
  }

  public void clear() {
    sessions.clear();
    precheckIndex.clear();
  }
}
