package com.transwarp.serviceinsight.knowledge.governance.port;

import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.CommandResult;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.GovernanceCommand;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.IdempotencyRecord;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.NewRevision;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.RevisionCreated;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.VersionState;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeGovernanceRepository {
  void lockIdempotency(String commandType, String idempotencyKey);

  Optional<IdempotencyRecord> findIdempotency(String commandType, String idempotencyKey);

  Optional<VersionState> lockVersion(UUID versionId);

  RevisionCreated createRevision(NewRevision revision);

  CommandResult apply(GovernanceCommand command);
}
