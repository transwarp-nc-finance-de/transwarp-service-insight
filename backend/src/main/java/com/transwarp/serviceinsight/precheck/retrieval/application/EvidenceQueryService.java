package com.transwarp.serviceinsight.precheck.retrieval.application;

import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.EvidenceDocument;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.EvidenceView;
import com.transwarp.serviceinsight.precheck.retrieval.port.RetrievalAuditPort;
import com.transwarp.serviceinsight.precheck.v2.application.PrecheckV2Exception;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EvidenceQueryService {
  private final RetrievalAuditPort repository;
  private final AuthSessionApplicationService authSessions;

  public EvidenceQueryService(
      RetrievalAuditPort repository, AuthSessionApplicationService authSessions) {
    this.repository = repository;
    this.authSessions = authSessions;
  }

  public EvidenceView get(UUID evidenceId, String sessionCookie) {
    var auth = authSessions.current(sessionCookie);
    var evidence = repository.findEvidence(evidenceId).orElseThrow(this::notFound);
    if (!auth.identity().hasRole(Role.PRECHECK_USER)
        || !auth.identity().canAccessProductLine(evidence.snapshot().productLineCode())) {
      throw notFound();
    }
    var snapshot = evidence.snapshot();
    return new EvidenceView(
        snapshot.evidenceId(),
        new EvidenceDocument(snapshot.documentId(), snapshot.title()),
        snapshot.versionId(),
        snapshot.chunkId(),
        snapshot.excerpt(),
        snapshot.contentHash(),
        true);
  }

  private PrecheckV2Exception notFound() {
    return new PrecheckV2Exception("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "资源不存在", Map.of());
  }
}
