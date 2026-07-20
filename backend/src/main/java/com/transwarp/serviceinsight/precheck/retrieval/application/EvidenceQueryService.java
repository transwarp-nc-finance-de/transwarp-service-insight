package com.transwarp.serviceinsight.precheck.retrieval.application;

import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StoredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StructuredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.port.StructuredAuditPort;
import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.EvidenceDocument;
import com.transwarp.serviceinsight.precheck.retrieval.domain.RetrievalModels.EvidenceView;
import com.transwarp.serviceinsight.precheck.retrieval.port.RetrievalAuditPort;
import com.transwarp.serviceinsight.precheck.v2.application.PrecheckV2Exception;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EvidenceQueryService {
  private final RetrievalAuditPort repository;
  private final AuthSessionApplicationService authSessions;
  private final StructuredAuditPort audit;
  private final Clock clock;

  public EvidenceQueryService(
      RetrievalAuditPort repository,
      AuthSessionApplicationService authSessions,
      StructuredAuditPort audit,
      Clock clock) {
    this.repository = repository;
    this.authSessions = authSessions;
    this.audit = audit;
    this.clock = clock;
  }

  public EvidenceView get(UUID evidenceId, String sessionCookie) {
    var auth = authSessions.current(sessionCookie);
    var evidence = repository.findEvidence(evidenceId).orElseThrow(this::notFound);
    if (!auth.identity().hasRole(Role.PRECHECK_USER)
        || !auth.identity().canAccessProductLine(evidence.snapshot().productLineCode())) {
      throw notFound();
    }
    var snapshot = evidence.snapshot();
    audit.record(
        new StoredAuditEvent(
            new StructuredAuditEvent(
                UUID.randomUUID(),
                auth.identity().userCode(),
                "EVIDENCE_VIEWED",
                "Evidence",
                evidenceId,
                "SUCCEEDED",
                Map.of(
                    "versionId", snapshot.versionId().toString(),
                    "chunkId", snapshot.chunkId().toString(),
                    "productLineCode", snapshot.productLineCode()),
                clock.instant(),
                true),
            snapshot.productLineCode(),
            null));
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
