package com.transwarp.serviceinsight.precheck.v2.api;

import com.transwarp.serviceinsight.precheck.v2.api.PersistentPrecheckRequests.ConfirmationReason;
import com.transwarp.serviceinsight.precheck.v2.api.PersistentPrecheckRequests.CreateSessionRequest;
import com.transwarp.serviceinsight.precheck.v2.application.PersistentPrecheckService;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/precheck-sessions")
public class PersistentPrecheckController {
  private final PersistentPrecheckService service;

  public PersistentPrecheckController(PersistentPrecheckService service) {
    this.service = service;
  }

  @GetMapping
  com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckSessionPage list(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "updatedAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String sortDirection,
      @RequestParam(required = false) String status) {
    return service.listSessions(sessionCookie, page, size, sortBy, sortDirection, status);
  }

  @GetMapping("/{sessionId}")
  PrecheckSession get(
      @PathVariable java.util.UUID sessionId,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.getSession(sessionId, sessionCookie);
  }

  @GetMapping("/{sessionId}/runs")
  com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckRunPage listRuns(
      @PathVariable java.util.UUID sessionId,
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "sequence") String sortBy,
      @RequestParam(defaultValue = "DESC") String sortDirection) {
    return service.listRuns(sessionId, sessionCookie, page, size, sortBy, sortDirection);
  }

  @GetMapping("/{sessionId}/runs/{runId}")
  com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckRun getRun(
      @PathVariable java.util.UUID sessionId,
      @PathVariable java.util.UUID runId,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.getRun(sessionId, runId, sessionCookie);
  }

  @PostMapping
  ResponseEntity<PrecheckSession> create(
      @Valid @RequestBody CreateSessionRequest request,
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken) {
    var result = service.create(request.toDomain(), sessionCookie, csrfToken);
    return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
        .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
        .body(result.session());
  }

  @PostMapping("/{sessionId}/runs")
  ResponseEntity<
          com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckRun>
      createRun(
          @PathVariable java.util.UUID sessionId,
          @Valid @RequestBody CreateSessionRequest request,
          @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
          @CookieValue(name = "SESSION", required = false) String sessionCookie,
          @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken) {
    var result =
        service.createRun(sessionId, request.toDomain(), idempotencyKey, sessionCookie, csrfToken);
    return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
        .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
        .body(result.run());
  }

  @PostMapping("/{sessionId}/self-service-confirmations")
  ResponseEntity<
          com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels
              .SessionTermination>
      confirmSelfService(
          @PathVariable java.util.UUID sessionId,
          @Valid @RequestBody ConfirmationReason request,
          @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
          @CookieValue(name = "SESSION", required = false) String sessionCookie,
          @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken) {
    var result =
        service.confirmSelfService(
            sessionId, request.reason(), idempotencyKey, sessionCookie, csrfToken);
    return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
        .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
        .body(result.termination());
  }
}
