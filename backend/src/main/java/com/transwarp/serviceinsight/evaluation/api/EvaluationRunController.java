package com.transwarp.serviceinsight.evaluation.api;

import com.transwarp.serviceinsight.evaluation.application.EvaluationRunProcessor;
import com.transwarp.serviceinsight.evaluation.application.EvaluationRunService;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.EvaluationFailurePage;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.EvaluationRun;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.EvaluationRunPage;
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
@RequestMapping("/api/v2/evaluation-runs")
public class EvaluationRunController {
  private final EvaluationRunService service;
  private final EvaluationRunProcessor processor;

  public EvaluationRunController(EvaluationRunService service, EvaluationRunProcessor processor) {
    this.service = service;
    this.processor = processor;
  }

  @PostMapping
  ResponseEntity<EvaluationRun> create(
      @RequestBody CreateEvaluationRunRequest request,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken) {
    var result =
        service.create(
            request.evaluationSetVersion(),
            request.note(),
            idempotencyKey,
            sessionCookie,
            csrfToken);
    if (!result.replayed()) processor.enqueue(result.run().taskId());
    return ResponseEntity.status(result.replayed() ? 200 : 202)
        .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
        .body(result.run());
  }

  record CreateEvaluationRunRequest(String evaluationSetVersion, String note) {}

  @GetMapping("/{taskId}")
  EvaluationRun get(
      @PathVariable java.util.UUID taskId,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.get(taskId, sessionCookie);
  }

  @GetMapping
  EvaluationRunPage list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDirection,
      @RequestParam(required = false) String status,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.list(page, size, sortBy, sortDirection, status, sessionCookie);
  }

  @GetMapping("/{taskId}/failures")
  EvaluationFailurePage failures(
      @PathVariable java.util.UUID taskId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "asc") String sortDirection,
      @RequestParam(required = false) String failedCheck,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.failures(taskId, page, size, sortDirection, failedCheck, sessionCookie);
  }
}
