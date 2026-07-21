package com.transwarp.serviceinsight.evaluation.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StoredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StructuredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.port.StructuredAuditPort;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.*;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.CreationResult;
import com.transwarp.serviceinsight.evaluation.domain.EvaluationModels.EvaluationRun;
import com.transwarp.serviceinsight.evaluation.infrastructure.ClasspathEvaluationSetCatalog;
import com.transwarp.serviceinsight.evaluation.port.EvaluationRunRepository;
import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.precheck.v2.application.PrecheckV2Exception;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Clock;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationRunService {
  private final EvaluationRunRepository repository;
  private final ClasspathEvaluationSetCatalog catalog;
  private final AuthSessionApplicationService authSessions;
  private final StructuredAuditPort audit;
  private final Clock clock;
  private final ObjectMapper objectMapper;

  public EvaluationRunService(
      EvaluationRunRepository repository,
      ClasspathEvaluationSetCatalog catalog,
      AuthSessionApplicationService authSessions,
      StructuredAuditPort audit,
      Clock clock,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.catalog = catalog;
    this.authSessions = authSessions;
    this.audit = audit;
    this.clock = clock;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public CreationResult create(
      String evaluationSetVersion,
      String note,
      String idempotencyKey,
      String sessionCookie,
      String csrfToken) {
    validateIdempotencyKey(idempotencyKey);
    validateRequest(evaluationSetVersion, note);
    var auth = authSessions.requireWriteSession(sessionCookie, csrfToken);
    requireAdmin(auth.identity().hasRole(Role.ADMIN));
    try {
      catalog.load(evaluationSetVersion);
    } catch (IllegalArgumentException exception) {
      throw validation("evaluationSetVersion", "评估集版本不受支持");
    }
    var normalizedNote = normalize(note);
    var requestHash = hash(evaluationSetVersion + ":" + normalizedNote);
    repository.lockIdempotency(idempotencyKey);
    var replay = repository.findIdempotency(idempotencyKey);
    if (replay.isPresent()) {
      if (!replay.get().requestHash().equals(requestHash)) {
        throw new PrecheckV2Exception(
            "IDEMPOTENCY_KEY_CONFLICT",
            HttpStatus.CONFLICT,
            "幂等键已用于不同请求",
            Map.of("mockData", true));
      }
      return new CreationResult(replay.get().run(), true);
    }
    var now = clock.instant();
    var taskId = UUID.randomUUID();
    var run =
        new EvaluationRun(
            taskId,
            UUID.nameUUIDFromBytes(evaluationSetVersion.getBytes(StandardCharsets.UTF_8)),
            "PENDING",
            0,
            3,
            null,
            null,
            now,
            null,
            null,
            true,
            evaluationSetVersion,
            null);
    repository.create(
        run,
        normalizedNote.isEmpty() ? null : normalizedNote,
        auth.identity().userCode(),
        idempotencyKey,
        requestHash,
        now);
    audit.record(
        new StoredAuditEvent(
            new StructuredAuditEvent(
                UUID.randomUUID(),
                auth.identity().userCode(),
                "EVALUATION_RUN_CREATED",
                "EvaluationRun",
                taskId,
                "SUCCEEDED",
                Map.of("evaluationSetVersion", evaluationSetVersion, "mockData", true),
                now,
                true),
            null,
            null));
    return new CreationResult(run, false);
  }

  @Transactional(readOnly = true)
  public EvaluationRun get(UUID taskId, String sessionCookie) {
    var auth = authSessions.current(sessionCookie);
    requireAdmin(auth.identity().hasRole(Role.ADMIN));
    return repository
        .findById(taskId)
        .orElseThrow(
            () ->
                new PrecheckV2Exception(
                    "EVALUATION_RUN_NOT_FOUND",
                    HttpStatus.NOT_FOUND,
                    "评估任务不存在",
                    Map.of("taskId", taskId, "mockData", true)));
  }

  @Transactional(readOnly = true)
  public EvaluationRunPage list(
      int page, int size, String sortBy, String direction, String status, String sessionCookie) {
    requireAdmin(authSessions.current(sessionCookie).identity().hasRole(Role.ADMIN));
    validatePage(page, size, sortBy, direction, List.of("createdAt", "status"));
    if (status != null && !List.of("PENDING", "RUNNING", "SUCCEEDED", "FAILED").contains(status))
      throw validation("status", "任务状态不合法");
    Comparator<EvaluationRun> comparator =
        "status".equals(sortBy)
            ? Comparator.comparing(EvaluationRun::status)
            : Comparator.comparing(EvaluationRun::createdAt);
    if ("desc".equalsIgnoreCase(direction)) comparator = comparator.reversed();
    var values =
        repository.findAll().stream()
            .filter(item -> status == null || status.equals(item.status()))
            .sorted(comparator.thenComparing(EvaluationRun::taskId))
            .toList();
    var items = slice(values, page, size);
    return new EvaluationRunPage(items, metadata(page, size, values.size(), sortBy, direction));
  }

  @Transactional(readOnly = true)
  public EvaluationFailurePage failures(
      UUID taskId, int page, int size, String direction, String failedCheck, String sessionCookie) {
    requireAdmin(authSessions.current(sessionCookie).identity().hasRole(Role.ADMIN));
    validatePage(page, size, "caseId", direction, List.of("caseId"));
    var run = repository.findById(taskId).orElseThrow(this::notFound);
    if (!"SUCCEEDED".equals(run.status())) {
      throw new PrecheckV2Exception(
          "ILLEGAL_STATE_TRANSITION", HttpStatus.CONFLICT, "评估任务尚未正常完成", Map.of("mockData", true));
    }
    var values =
        repository.findFailures(taskId).stream()
            .filter(item -> failedCheck == null || item.failedChecks().contains(failedCheck))
            .sorted(
                "desc".equalsIgnoreCase(direction)
                    ? Comparator.comparing(StoredFailure::caseId).reversed()
                    : Comparator.comparing(StoredFailure::caseId))
            .map(this::failure)
            .toList();
    return new EvaluationFailurePage(
        slice(values, page, size), metadata(page, size, values.size(), "caseId", direction));
  }

  private EvaluationFailure failure(StoredFailure item) {
    try {
      return new EvaluationFailure(
          item.caseId(),
          item.scenarioTags(),
          item.failedChecks(),
          item.failureCodes(),
          objectMapper.readValue(item.expectedSummary(), new TypeReference<>() {}),
          objectMapper.readValue(item.actualSummary(), new TypeReference<>() {}),
          true);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Stored evaluation result is invalid", exception);
    }
  }

  private <T> List<T> slice(List<T> values, int page, int size) {
    int start = Math.min(page * size, values.size());
    return values.subList(start, Math.min(start + size, values.size()));
  }

  private PageMetadata metadata(int page, int size, long total, String sortBy, String direction) {
    return new PageMetadata(
        page,
        size,
        total,
        total == 0 ? 0 : (int) Math.ceil((double) total / size),
        sortBy,
        direction.toLowerCase());
  }

  private void validatePage(
      int page, int size, String sortBy, String direction, List<String> allowedSorts) {
    if (page < 0 || size < 1 || size > 100) throw validation("page", "分页参数不合法");
    if (!allowedSorts.contains(sortBy)) throw validation("sortBy", "排序字段不合法");
    if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction))
      throw validation("sortDirection", "排序方向不合法");
  }

  private PrecheckV2Exception notFound() {
    return new PrecheckV2Exception("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "资源不存在", Map.of());
  }

  private void validateRequest(String version, String note) {
    if (version == null || version.isBlank() || version.length() > 64) {
      throw validation("evaluationSetVersion", "评估集版本不合法");
    }
    if (note != null && note.length() > 1000) throw validation("note", "备注长度不能超过 1000");
  }

  private void validateIdempotencyKey(String value) {
    if (value == null || value.length() < 8 || value.length() > 128) {
      throw validation("Idempotency-Key", "幂等键长度必须为 8 到 128");
    }
  }

  private void requireAdmin(boolean allowed) {
    if (!allowed) {
      throw new PrecheckV2Exception(
          "INSUFFICIENT_ROLE", HttpStatus.FORBIDDEN, "当前身份无权运行评估", Map.of("mockData", true));
    }
  }

  private PrecheckV2Exception validation(String field, String message) {
    return new PrecheckV2Exception(
        "VALIDATION_ERROR",
        HttpStatus.BAD_REQUEST,
        message,
        Map.of("field", field, "mockData", true));
  }

  private String normalize(String value) {
    return value == null
        ? ""
        : Normalizer.normalize(
            value.replace("\r\n", "\n").replace('\r', '\n').trim(), Normalizer.Form.NFC);
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
