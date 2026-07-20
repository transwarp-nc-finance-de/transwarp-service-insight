package com.transwarp.serviceinsight.precheck.interaction.api;

import com.transwarp.serviceinsight.precheck.interaction.application.InteractionService;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.AdoptionStatus;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.Feedback;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.Helpfulness;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.SubmissionContinuation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2")
public class InteractionController {
  private final InteractionService service;

  public InteractionController(InteractionService service) {
    this.service = service;
  }

  @PostMapping("/feedback")
  ResponseEntity<Feedback> createFeedback(
      @Valid @RequestBody CreateFeedbackRequest request,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken) {
    var result =
        service.createFeedback(
            request.sessionId(),
            request.runId(),
            request.adoptionStatus().name(),
            request.helpfulness() == null ? null : request.helpfulness().name(),
            request.reason(),
            idempotencyKey,
            sessionCookie,
            csrfToken);
    return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
        .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
        .body(result.feedback());
  }

  @GetMapping("/feedback/{feedbackId}")
  Feedback getFeedback(
      @PathVariable UUID feedbackId,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.getFeedback(feedbackId, sessionCookie);
  }

  @PostMapping("/submission-continuations")
  ResponseEntity<SubmissionContinuation> createContinuation(
      @Valid @RequestBody CreateContinuationRequest request,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken) {
    var result =
        service.createContinuation(
            request.sessionId(),
            Boolean.TRUE.equals(request.confirmed()),
            request.reason(),
            idempotencyKey,
            sessionCookie,
            csrfToken);
    return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
        .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
        .body(result.continuation());
  }

  @GetMapping("/submission-continuations/{continuationId}")
  SubmissionContinuation getContinuation(
      @PathVariable UUID continuationId,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.getContinuation(continuationId, sessionCookie);
  }

  record CreateFeedbackRequest(
      @NotNull UUID sessionId,
      @NotNull UUID runId,
      @NotNull AdoptionStatus adoptionStatus,
      Helpfulness helpfulness,
      @Size(max = 2000) String reason) {}

  record CreateContinuationRequest(
      @NotNull UUID sessionId,
      @NotNull @AssertTrue Boolean confirmed,
      @Size(max = 2000) String reason) {}
}
