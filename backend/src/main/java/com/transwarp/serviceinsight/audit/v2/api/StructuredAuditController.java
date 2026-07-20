package com.transwarp.serviceinsight.audit.v2.api;

import com.transwarp.serviceinsight.audit.v2.application.StructuredAuditQueryService;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.AuditEventPage;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StructuredAuditEvent;
import java.util.UUID;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/audit-events")
public class StructuredAuditController {
  private final StructuredAuditQueryService service;

  public StructuredAuditController(StructuredAuditQueryService service) {
    this.service = service;
  }

  @GetMapping
  AuditEventPage list(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "occurredAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String sortDirection,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) UUID subjectId) {
    return service.list(sessionCookie, page, size, sortBy, sortDirection, action, subjectId);
  }

  @GetMapping("/{eventId}")
  StructuredAuditEvent get(
      @CookieValue(name = "SESSION", required = false) String sessionCookie,
      @PathVariable UUID eventId) {
    return service.get(sessionCookie, eventId);
  }
}
