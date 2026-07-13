package com.transwarp.serviceinsight.knowledge.port;

import com.transwarp.serviceinsight.knowledge.domain.KnowledgeDocument;

public interface DataAccessPolicy {
  boolean allows(KnowledgeDocument document);
}
