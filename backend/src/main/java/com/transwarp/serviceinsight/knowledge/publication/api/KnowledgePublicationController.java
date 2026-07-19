package com.transwarp.serviceinsight.knowledge.publication.api;

import com.transwarp.serviceinsight.knowledge.governance.api.RequiredCommandReasonRequest;
import com.transwarp.serviceinsight.knowledge.governance.application.KnowledgeGovernanceService;
import com.transwarp.serviceinsight.knowledge.publication.application.KnowledgePublicationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/knowledge-versions/{versionId}")
public class KnowledgePublicationController {
  private final KnowledgePublicationService service;
  private final KnowledgeGovernanceService governanceService;

  public KnowledgePublicationController(
      KnowledgePublicationService service, KnowledgeGovernanceService governanceService) {
    this.service = service;
    this.governanceService = governanceService;
  }

  @PostMapping("/publications")
  ResponseEntity<?> publish(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable UUID versionId,
      @Valid @RequestBody CommandReasonRequest request) {
    var result = service.publish(sessionCookie, csrfToken, idempotencyKey, versionId, request);
    return ResponseEntity.accepted()
        .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
        .body(result.task());
  }

  @PostMapping("/deprecations")
  ResponseEntity<?> deprecate(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable UUID versionId,
      @Valid @RequestBody RequiredCommandReasonRequest request) {
    var result =
        governanceService.deprecate(sessionCookie, csrfToken, idempotencyKey, versionId, request);
    return ResponseEntity.ok()
        .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
        .body(result.body());
  }
}
