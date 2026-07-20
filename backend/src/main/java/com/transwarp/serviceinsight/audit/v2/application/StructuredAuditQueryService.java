package com.transwarp.serviceinsight.audit.v2.application;

import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.AuditEventPage;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.PageMetadata;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StructuredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.port.StructuredAuditPort;
import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.precheck.v2.application.PrecheckV2Exception;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class StructuredAuditQueryService {
  private final StructuredAuditPort audit;
  private final AuthSessionApplicationService authSessions;

  public StructuredAuditQueryService(
      StructuredAuditPort audit, AuthSessionApplicationService authSessions) {
    this.audit = audit;
    this.authSessions = authSessions;
  }

  public AuditEventPage list(
      String sessionCookie,
      int page,
      int size,
      String sortBy,
      String sortDirection,
      String action,
      UUID subjectId) {
    validatePage(page, size);
    validateSort(sortBy, sortDirection);
    var identity = authSessions.current(sessionCookie).identity();
    requireAdmin(identity.hasRole(Role.ADMIN));
    var total = audit.count(identity.productLineCodes(), action, subjectId);
    var items =
        audit
            .find(
                identity.productLineCodes(),
                action,
                subjectId,
                (page - 1) * size,
                size,
                sortBy,
                sortDirection)
            .stream()
            .map(stored -> stored.event())
            .toList();
    var totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
    return new AuditEventPage(
        items, new PageMetadata(page, size, total, totalPages, sortBy, sortDirection));
  }

  public StructuredAuditEvent get(String sessionCookie, UUID eventId) {
    var identity = authSessions.current(sessionCookie).identity();
    requireAdmin(identity.hasRole(Role.ADMIN));
    return audit
        .findById(eventId, identity.productLineCodes())
        .map(stored -> stored.event())
        .orElseThrow(this::notFound);
  }

  private void validatePage(int page, int size) {
    if (page < 1 || size < 1 || size > 100) {
      throw new PrecheckV2Exception(
          "VALIDATION_ERROR",
          HttpStatus.BAD_REQUEST,
          "分页参数不合法",
          Map.of("field", "page", "mockData", true));
    }
  }

  private void validateSort(String sortBy, String direction) {
    if (!List.of("occurredAt", "action").contains(sortBy)
        || !List.of("ASC", "DESC").contains(direction)) {
      throw new PrecheckV2Exception(
          "VALIDATION_ERROR",
          HttpStatus.BAD_REQUEST,
          "排序参数不合法",
          Map.of("field", "sortBy", "mockData", true));
    }
  }

  private void requireAdmin(boolean allowed) {
    if (!allowed) {
      throw new PrecheckV2Exception(
          "INSUFFICIENT_ROLE", HttpStatus.FORBIDDEN, "当前身份无权读取结构化审计", Map.of("mockData", true));
    }
  }

  private PrecheckV2Exception notFound() {
    return new PrecheckV2Exception("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "资源不存在", Map.of());
  }
}
