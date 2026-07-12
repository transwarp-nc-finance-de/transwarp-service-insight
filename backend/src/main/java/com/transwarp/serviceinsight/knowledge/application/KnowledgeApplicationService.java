package com.transwarp.serviceinsight.knowledge.application;

import com.transwarp.serviceinsight.knowledge.domain.KnowledgeStatus;
import com.transwarp.serviceinsight.knowledge.domain.KnowledgeVersion;
import com.transwarp.serviceinsight.knowledge.port.KnowledgeRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeApplicationService implements KnowledgeLifecycleUseCase {
  private final KnowledgeRepository repository;

  public KnowledgeApplicationService(KnowledgeRepository repository) {
    this.repository = repository;
  }

  @Override
  public KnowledgeVersion transition(UUID versionId, KnowledgeStatus nextStatus) {
    var current =
        repository.findById(versionId).orElseThrow(() -> new IllegalArgumentException("模拟知识版本不存在"));
    var updated = current.transitionTo(nextStatus);
    repository.save(updated);
    return updated;
  }
}
