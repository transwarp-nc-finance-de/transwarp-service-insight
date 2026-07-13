package com.transwarp.serviceinsight.knowledge.infrastructure.mock;

import com.transwarp.serviceinsight.knowledge.domain.KnowledgeDocument;
import com.transwarp.serviceinsight.knowledge.port.DataAccessPolicy;
import org.springframework.stereotype.Component;

@Component
public class MockDataAccessPolicy implements DataAccessPolicy {
  @Override
  public boolean allows(KnowledgeDocument document) {
    return document.authorizationStatus() == KnowledgeDocument.AuthorizationStatus.APPROVED
        && (document.dataClassification() == KnowledgeDocument.DataClassification.MOCK
            || document.dataClassification() == KnowledgeDocument.DataClassification.PUBLIC);
  }
}
