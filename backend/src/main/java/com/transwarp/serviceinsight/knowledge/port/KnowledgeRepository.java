package com.transwarp.serviceinsight.knowledge.port;

import com.transwarp.serviceinsight.knowledge.domain.KnowledgeVersion;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeRepository {
  void save(KnowledgeVersion version);

  Optional<KnowledgeVersion> findById(UUID id);
}
