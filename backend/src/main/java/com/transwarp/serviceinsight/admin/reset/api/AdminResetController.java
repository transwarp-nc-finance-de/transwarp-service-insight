package com.transwarp.serviceinsight.admin.reset.api;

import com.transwarp.serviceinsight.admin.reset.application.AdminResetProcessor;
import com.transwarp.serviceinsight.admin.reset.application.AdminResetService;
import com.transwarp.serviceinsight.admin.reset.domain.AdminResetModels.AdminReset;
import com.transwarp.serviceinsight.admin.reset.domain.AdminResetModels.AdminResetPage;
import java.util.UUID;
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
@RequestMapping("/api/v2/admin/resets")
public class AdminResetController {
  private final AdminResetService service;
  private final AdminResetProcessor processor;

  public AdminResetController(AdminResetService service, AdminResetProcessor processor) {
    this.service = service;
    this.processor = processor;
  }

  @PostMapping
  ResponseEntity<AdminReset> create(
      @RequestBody CreateAdminResetRequest request,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestHeader(name = "X-CSRF-Token", required = false) String csrfToken) {
    var result =
        service.create(
            request.environmentCode(),
            request.confirmationPhrase(),
            request.reason(),
            idempotencyKey,
            sessionCookie,
            csrfToken);
    if (!result.replayed()) processor.enqueue(result.reset().taskId());
    return ResponseEntity.status(result.replayed() ? 200 : 202)
        .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
        .body(result.reset());
  }

  @GetMapping
  AdminResetPage list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDirection,
      @RequestParam(required = false) String status,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.list(page, size, sortBy, sortDirection, status, sessionCookie);
  }

  @GetMapping("/{resetId}")
  AdminReset get(
      @PathVariable UUID resetId,
      @CookieValue(name = "SESSION", required = false) String sessionCookie) {
    return service.get(resetId, sessionCookie);
  }

  record CreateAdminResetRequest(
      String environmentCode, String confirmationPhrase, String reason) {}
}
