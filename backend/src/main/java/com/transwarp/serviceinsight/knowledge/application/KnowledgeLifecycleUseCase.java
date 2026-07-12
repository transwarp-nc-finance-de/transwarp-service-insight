package com.transwarp.serviceinsight.knowledge.application;

import com.transwarp.serviceinsight.knowledge.domain.KnowledgeStatus;
import com.transwarp.serviceinsight.knowledge.domain.KnowledgeVersion;
import java.util.UUID;

public interface KnowledgeLifecycleUseCase {
  KnowledgeVersion transition(UUID versionId, KnowledgeStatus nextStatus);
}
