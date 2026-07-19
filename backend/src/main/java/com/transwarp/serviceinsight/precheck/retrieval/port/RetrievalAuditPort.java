package com.transwarp.serviceinsight.precheck.retrieval.port;

import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.RetrievalOutcome;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.StoredEvidence;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RetrievalAuditPort {
  void save(UUID runId, String ownerUserCode, RetrievalOutcome outcome, Instant createdAt);

  Optional<StoredEvidence> findEvidence(UUID evidenceId);
}
