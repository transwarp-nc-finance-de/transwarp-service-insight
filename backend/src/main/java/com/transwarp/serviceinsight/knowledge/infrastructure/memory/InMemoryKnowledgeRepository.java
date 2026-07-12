package com.transwarp.serviceinsight.knowledge.infrastructure.memory;

import com.transwarp.serviceinsight.knowledge.domain.KnowledgeVersion;
import com.transwarp.serviceinsight.knowledge.port.KnowledgeRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryKnowledgeRepository implements KnowledgeRepository {
  private final ConcurrentHashMap<UUID, KnowledgeVersion> versions = new ConcurrentHashMap<>();

  @Override
  public void save(KnowledgeVersion version) {
    versions.put(version.id(), version);
  }

  @Override
  public Optional<KnowledgeVersion> findById(UUID id) {
    return Optional.ofNullable(versions.get(id));
  }
}
