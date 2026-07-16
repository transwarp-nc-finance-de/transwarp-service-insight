package com.transwarp.serviceinsight.knowledge.governance.api;

import com.transwarp.serviceinsight.knowledge.governance.application.KnowledgeGovernanceService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/knowledge-versions/{versionId}")
public class KnowledgeGovernanceController {
  private final KnowledgeGovernanceService service;

  public KnowledgeGovernanceController(KnowledgeGovernanceService service) {
    this.service = service;
  }

  @PostMapping(path = "/revisions", consumes = "multipart/form-data")
  ResponseEntity<?> revise(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable UUID versionId,
      @Valid @RequestPart("revision") KnowledgeRevisionRequest request) {
    var result = service.revise(sessionCookie, csrfToken, idempotencyKey, versionId, request);
    return ResponseEntity.status(201)
        .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
        .body(result.body());
  }

  @PostMapping("/review-submissions")
  ResponseEntity<?> submit(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable UUID versionId,
      @Valid @RequestBody ReviewSubmissionRequest request) {
    var result = service.submit(sessionCookie, csrfToken, idempotencyKey, versionId, request);
    return ok(result);
  }

  @PostMapping("/draft-returns")
  ResponseEntity<?> returnToDraft(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable UUID versionId,
      @Valid @RequestBody RequiredCommandReasonRequest request) {
    var result =
        service.returnToDraft(sessionCookie, csrfToken, idempotencyKey, versionId, request);
    return ok(result);
  }

  @PostMapping("/approvals")
  ResponseEntity<?> approve(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable UUID versionId,
      @Valid @RequestBody KnowledgeApprovalRequest request) {
    var result = service.approve(sessionCookie, csrfToken, idempotencyKey, versionId, request);
    return ok(result);
  }

  private ResponseEntity<?> ok(KnowledgeGovernanceService.CommandResponse result) {
    return ResponseEntity.ok()
        .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
        .body(result.body());
  }
}
