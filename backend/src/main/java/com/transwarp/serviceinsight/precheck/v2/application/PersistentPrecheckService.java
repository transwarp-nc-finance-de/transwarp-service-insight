package com.transwarp.serviceinsight.precheck.v2.application;

import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StoredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StructuredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.port.StructuredAuditPort;
import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.precheck.retrieval.application.AuthorizedRetrievalService;
import com.transwarp.serviceinsight.precheck.retrieval.port.RetrievalAuditPort;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.CompletenessPolicy;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.CompletenessPolicyPage;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.CreationResult;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PageMetadata;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckContext;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckRun;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckRunPage;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckSession;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckSessionPage;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.RunCreationResult;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.TerminationResult;
import com.transwarp.serviceinsight.precheck.v2.port.PersistentPrecheckRepository;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersistentPrecheckService {
  private static final int MAX_RUNS = 3;

  private final PersistentPrecheckRepository repository;
  private final AuthSessionApplicationService authSessions;
  private final PrecheckContextNormalizer contextNormalizer;
  private final DeterministicPrecheckPolicy precheckPolicy;
  private final AuthorizedRetrievalService retrieval;
  private final RetrievalAuditPort retrievalAudit;
  private final StructuredAuditPort audit;
  private final Clock clock;

  public PersistentPrecheckService(
      PersistentPrecheckRepository repository,
      AuthSessionApplicationService authSessions,
      PrecheckContextNormalizer contextNormalizer,
      AuthorizedRetrievalService retrieval,
      RetrievalAuditPort retrievalAudit,
      StructuredAuditPort audit,
      Clock clock) {
    this.repository = repository;
    this.authSessions = authSessions;
    this.contextNormalizer = contextNormalizer;
    this.precheckPolicy = new DeterministicPrecheckPolicy();
    this.retrieval = retrieval;
    this.retrievalAudit = retrievalAudit;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional
  public CreationResult create(PrecheckContext context, String sessionCookie, String csrfToken) {
    var auth = authSessions.requireWriteSession(sessionCookie, csrfToken);
    requirePrecheckUser(auth.identity().hasRole(Role.PRECHECK_USER));
    requireProductLine(auth.identity().canAccessProductLine(context.productLine().code()));
    validateContext(context);
    var hash = contextNormalizer.contextHash(context);
    repository.lockBusinessKey(context.sourceSystem(), context.hostRequestId());
    var existing = repository.findBusinessKey(context.sourceSystem(), context.hostRequestId());
    if (existing.isPresent()) {
      var existingSession = repository.findSession(existing.get().sessionId()).orElseThrow();
      if (!existingSession.ownerUserCode().equals(auth.identity().userCode())
          || !auth.identity()
              .canAccessProductLine(
                  existingSession.latestRun().contextSnapshot().productLine().code())) {
        throw conflict(
            "IDEMPOTENCY_CONTEXT_CONFLICT", "宿主请求已关联其他上下文，原记录未覆盖", Map.of("mockData", true));
      }
      if (!existing.get().contextHash().equals(hash)) {
        throw conflict(
            "IDEMPOTENCY_CONTEXT_CONFLICT", "宿主请求已关联不同上下文，原记录未覆盖", Map.of("mockData", true));
      }
      return new CreationResult(existingSession, true);
    }

    var policy = repository.findPolicy(context.issueType().code());
    var now = clock.instant();
    var sessionId = UUID.randomUUID();
    var runId = UUID.randomUUID();
    var retrievalResult = retrieval.retrieve(auth.identity(), context);
    var result = precheckPolicy.evaluate(context, policy, 1, retrievalResult);
    var run = new PrecheckRun(runId, sessionId, 1, runStatus(result), context, result, now, now);
    var session =
        new PrecheckSession(
            sessionId,
            auth.identity().userCode(),
            "ACTIVE",
            null,
            run,
            1,
            MAX_RUNS,
            now,
            now,
            true);
    repository.create(session, run, hash);
    retrievalAudit.save(runId, auth.identity().userCode(), retrievalResult, now);
    record(
        auth.identity().userCode(),
        "PRECHECK_SESSION_CREATED",
        "PrecheckSession",
        sessionId,
        context.productLine().code(),
        Map.of(
            "runId",
            runId.toString(),
            "policyVersion",
            result.completeness().policyVersion(),
            "degraded",
            result.retrieval().degraded()),
        now);
    record(
        auth.identity().userCode(),
        "PRECHECK_RUN_CREATED",
        "PrecheckRun",
        runId,
        context.productLine().code(),
        Map.of(
            "sessionId",
            sessionId.toString(),
            "sequence",
            1,
            "retrievalMode",
            result.retrieval().mode(),
            "degraded",
            result.retrieval().degraded()),
        now);
    return new CreationResult(session, false);
  }

  @Transactional
  public RunCreationResult createRun(
      UUID sessionId,
      PrecheckContext context,
      String idempotencyKey,
      String sessionCookie,
      String csrfToken) {
    validateIdempotencyKey(idempotencyKey);
    var auth = authSessions.requireWriteSession(sessionCookie, csrfToken);
    requirePrecheckUser(auth.identity().hasRole(Role.PRECHECK_USER));
    validateContext(context);
    var requestHash = contextNormalizer.runCommandHash(sessionId, context);
    repository.lockCommand("RUN", idempotencyKey);
    var replay = repository.findRunIdempotency(idempotencyKey);
    if (replay.isPresent()) {
      if (!replay.get().requestHash().equals(requestHash)) {
        throw conflict("IDEMPOTENCY_KEY_CONFLICT", "幂等键已用于不同请求", Map.of("mockData", true));
      }
      var replaySession =
          repository.lockSession(replay.get().run().sessionId()).orElseThrow(this::notFound);
      authorizeOwner(
          replaySession,
          auth.identity().userCode(),
          auth.identity()
              .canAccessProductLine(
                  replaySession.latestRun().contextSnapshot().productLine().code()));
      return new RunCreationResult(replay.get().run(), true);
    }
    var session = repository.lockSession(sessionId).orElseThrow(this::notFound);
    authorizeOwner(
        session,
        auth.identity().userCode(),
        auth.identity().canAccessProductLine(context.productLine().code()));
    if (!"ACTIVE".equals(session.status())) {
      throw conflict("ILLEGAL_STATE_TRANSITION", "终态 Session 只读", Map.of("mockData", true));
    }
    requireSameProblemIdentity(session.latestRun().contextSnapshot(), context);
    if (session.runCount() >= MAX_RUNS) {
      throw conflict(
          "RUN_LIMIT_REACHED",
          "已达到最多三次 Run",
          Map.of(
              "maxRuns",
              MAX_RUNS,
              "allowedActions",
              List.of("CONTINUE_SUBMISSION"),
              "mockData",
              true));
    }
    var policy = repository.findPolicy(context.issueType().code());
    var now = clock.instant();
    var sequence = session.runCount() + 1;
    var retrievalResult = retrieval.retrieve(auth.identity(), context);
    var result = precheckPolicy.evaluate(context, policy, sequence, retrievalResult);
    var run =
        new PrecheckRun(
            UUID.randomUUID(), sessionId, sequence, runStatus(result), context, result, now, now);
    repository.appendRun(run, idempotencyKey, requestHash);
    retrievalAudit.save(run.runId(), auth.identity().userCode(), retrievalResult, now);
    record(
        auth.identity().userCode(),
        "PRECHECK_RUN_CREATED",
        "PrecheckRun",
        run.runId(),
        context.productLine().code(),
        Map.of(
            "sessionId",
            sessionId.toString(),
            "sequence",
            sequence,
            "retrievalMode",
            result.retrieval().mode(),
            "degraded",
            result.retrieval().degraded()),
        now);
    return new RunCreationResult(run, false);
  }

  @Transactional
  public TerminationResult confirmSelfService(
      UUID sessionId,
      String reason,
      String idempotencyKey,
      String sessionCookie,
      String csrfToken) {
    validateIdempotencyKey(idempotencyKey);
    var auth = authSessions.requireWriteSession(sessionCookie, csrfToken);
    requirePrecheckUser(auth.identity().hasRole(Role.PRECHECK_USER));
    var requestHash = contextNormalizer.terminationCommandHash(sessionId, reason);
    repository.lockCommand("SELF_SERVICE", idempotencyKey);
    var replay = repository.findTerminationIdempotency(idempotencyKey);
    if (replay.isPresent()) {
      if (!replay.get().requestHash().equals(requestHash)) {
        throw conflict("IDEMPOTENCY_KEY_CONFLICT", "幂等键已用于不同请求", Map.of("mockData", true));
      }
      var replaySession =
          repository
              .lockSession(replay.get().termination().sessionId())
              .orElseThrow(this::notFound);
      authorizeOwner(
          replaySession,
          auth.identity().userCode(),
          auth.identity()
              .canAccessProductLine(
                  replaySession.latestRun().contextSnapshot().productLine().code()));
      return new TerminationResult(replay.get().termination(), true);
    }
    var session = repository.lockSession(sessionId).orElseThrow(this::notFound);
    authorizeOwner(
        session,
        auth.identity().userCode(),
        auth.identity()
            .canAccessProductLine(session.latestRun().contextSnapshot().productLine().code()));
    if (!"ACTIVE".equals(session.status())) {
      throw conflict("ILLEGAL_STATE_TRANSITION", "终态 Session 只读", Map.of("mockData", true));
    }
    var now = clock.instant();
    var termination = repository.terminate(sessionId, idempotencyKey, requestHash, now);
    record(
        auth.identity().userCode(),
        "SELF_SERVICE_CONFIRMED",
        "PrecheckSession",
        sessionId,
        session.latestRun().contextSnapshot().productLine().code(),
        Map.of("terminationReason", "SELF_SERVICE_CONFIRMED"),
        now);
    return new TerminationResult(termination, false);
  }

  public PrecheckSessionPage listSessions(
      String sessionCookie,
      int page,
      int size,
      String sortBy,
      String sortDirection,
      String status) {
    validatePage(page, size);
    validateSort(sortBy, List.of("createdAt", "updatedAt", "status"), sortDirection);
    if (status != null && !List.of("ACTIVE", "TERMINATED").contains(status)) {
      throw validation("status", "Session 状态不合法");
    }
    var auth = authSessions.current(sessionCookie);
    requirePrecheckUser(auth.identity().hasRole(Role.PRECHECK_USER));
    var visible =
        repository.findSessionsByOwner(auth.identity().userCode()).stream()
            .filter(
                session ->
                    auth.identity()
                        .canAccessProductLine(
                            session.latestRun().contextSnapshot().productLine().code()))
            .filter(session -> status == null || status.equals(session.status()))
            .sorted(sessionComparator(sortBy, sortDirection))
            .toList();
    var items = slice(visible, page, size);
    return new PrecheckSessionPage(
        items, metadata(page, size, visible.size(), sortBy, sortDirection));
  }

  public PrecheckSession getSession(UUID sessionId, String sessionCookie) {
    var auth = authSessions.current(sessionCookie);
    var session = repository.findSession(sessionId).orElseThrow(this::notFound);
    authorizeOwner(
        session,
        auth.identity().userCode(),
        auth.identity()
            .canAccessProductLine(session.latestRun().contextSnapshot().productLine().code()));
    requirePrecheckUser(auth.identity().hasRole(Role.PRECHECK_USER));
    return session;
  }

  public PrecheckRunPage listRuns(
      UUID sessionId,
      String sessionCookie,
      int page,
      int size,
      String sortBy,
      String sortDirection) {
    validatePage(page, size);
    validateSort(sortBy, List.of("sequence", "createdAt"), sortDirection);
    getSession(sessionId, sessionCookie);
    var runs =
        repository.findRuns(sessionId).stream()
            .sorted(runComparator(sortBy, sortDirection))
            .toList();
    return new PrecheckRunPage(
        slice(runs, page, size), metadata(page, size, runs.size(), sortBy, sortDirection));
  }

  public PrecheckRun getRun(UUID sessionId, UUID runId, String sessionCookie) {
    getSession(sessionId, sessionCookie);
    return repository.findRun(sessionId, runId).orElseThrow(this::notFound);
  }

  public CompletenessPolicyPage listPolicies(
      String sessionCookie,
      String issueTypeCode,
      int page,
      int size,
      String sortBy,
      String sortDirection) {
    validatePage(page, size);
    validateSort(sortBy, List.of("createdAt", "policyVersion"), sortDirection);
    if (issueTypeCode != null && !issueTypeCodes().contains(issueTypeCode)) {
      throw validation("issueTypeCode", "问题类型不合法");
    }
    var auth = authSessions.current(sessionCookie);
    requirePolicyReader(
        auth.identity().hasRole(Role.PRECHECK_USER) || auth.identity().hasRole(Role.ADMIN));
    var policies =
        repository.findPolicies(issueTypeCode).stream()
            .sorted(policyComparator(sortBy, sortDirection))
            .toList();
    return new CompletenessPolicyPage(
        slice(policies, page, size), metadata(page, size, policies.size(), sortBy, sortDirection));
  }

  public CompletenessPolicy getPolicy(String sessionCookie, String policyVersion) {
    var auth = authSessions.current(sessionCookie);
    requirePolicyReader(
        auth.identity().hasRole(Role.PRECHECK_USER) || auth.identity().hasRole(Role.ADMIN));
    return repository.findPolicyVersion(policyVersion).orElseThrow(this::notFound);
  }

  private void validateContext(PrecheckContext context) {
    if (!List.of("SANDBOX", "AIOPS").contains(context.sourceSystem())) {
      throw validation("sourceSystem", "仅支持 SANDBOX 或 AIOPS");
    }
    if (!issueTypeCodes().contains(context.issueType().code())) {
      throw validation("context.issueType.code", "问题类型不合法");
    }
  }

  private List<String> issueTypeCodes() {
    return List.of(
        "FUNCTIONAL_FAILURE",
        "PERFORMANCE_DEGRADATION",
        "INSTALLATION_CONFIGURATION",
        "DATA_CORRECTNESS");
  }

  private void validateIdempotencyKey(String value) {
    if (value == null || value.length() < 8 || value.length() > 128) {
      throw validation("Idempotency-Key", "幂等键长度必须为 8 到 128");
    }
  }

  private void validatePage(int page, int size) {
    if (page < 1 || size < 1 || size > 100) {
      throw validation("page", "分页参数不合法");
    }
  }

  private void validateSort(String sortBy, List<String> allowed, String direction) {
    if (!allowed.contains(sortBy)) {
      throw validation("sortBy", "排序字段不合法");
    }
    if (!List.of("ASC", "DESC").contains(direction)) {
      throw validation("sortDirection", "排序方向不合法");
    }
  }

  private Comparator<PrecheckSession> sessionComparator(String sortBy, String direction) {
    Comparator<PrecheckSession> comparator =
        switch (sortBy) {
          case "createdAt" -> Comparator.comparing(PrecheckSession::createdAt);
          case "status" -> Comparator.comparing(PrecheckSession::status);
          default -> Comparator.comparing(PrecheckSession::updatedAt);
        };
    return directed(comparator, direction).thenComparing(PrecheckSession::sessionId);
  }

  private String runStatus(
      com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckResult
          result) {
    if (result.retrieval().degraded()) return "DEGRADED";
    return result.completeness().complete() ? "COMPLETED" : "NEED_MORE_INFORMATION";
  }

  private Comparator<PrecheckRun> runComparator(String sortBy, String direction) {
    Comparator<PrecheckRun> comparator =
        "createdAt".equals(sortBy)
            ? Comparator.comparing(PrecheckRun::createdAt)
            : Comparator.comparingInt(PrecheckRun::sequence);
    return directed(comparator, direction).thenComparing(PrecheckRun::runId);
  }

  private Comparator<CompletenessPolicy> policyComparator(String sortBy, String direction) {
    Comparator<CompletenessPolicy> comparator =
        "policyVersion".equals(sortBy)
            ? Comparator.comparing(CompletenessPolicy::policyVersion)
            : Comparator.comparing(CompletenessPolicy::createdAt);
    return directed(comparator, direction).thenComparing(policy -> policy.issueType().code());
  }

  private <T> Comparator<T> directed(Comparator<T> comparator, String direction) {
    return "DESC".equals(direction) ? comparator.reversed() : comparator;
  }

  private <T> List<T> slice(List<T> values, int page, int size) {
    var from = Math.min((page - 1) * size, values.size());
    var to = Math.min(from + size, values.size());
    return values.subList(from, to);
  }

  private PageMetadata metadata(
      int page, int size, int totalItems, String sortBy, String direction) {
    var totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / size);
    return new PageMetadata(page, size, totalItems, totalPages, sortBy, direction);
  }

  private void authorizeOwner(
      PrecheckSession session, String userCode, boolean productLineAllowed) {
    if (!session.ownerUserCode().equals(userCode) || !productLineAllowed) throw notFound();
  }

  private void requireSameProblemIdentity(PrecheckContext previous, PrecheckContext next) {
    if (!previous.productLine().code().equals(next.productLine().code())
        || !code(previous.product()).equals(code(next.product()))
        || !code(previous.component()).equals(code(next.component()))) {
      throw conflict("NEW_SESSION_REQUIRED", "产品线、产品或组件变化需要新建 Session", Map.of("mockData", true));
    }
  }

  private String code(
      com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.CatalogValue value) {
    return value == null ? "" : value.code();
  }

  private PrecheckV2Exception notFound() {
    return new PrecheckV2Exception("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "资源不存在", Map.of());
  }

  private void requirePrecheckUser(boolean allowed) {
    if (!allowed) {
      throw new PrecheckV2Exception(
          "INSUFFICIENT_ROLE", HttpStatus.FORBIDDEN, "当前身份无权执行该操作", Map.of("mockData", true));
    }
  }

  private void requireProductLine(boolean allowed) {
    if (!allowed) {
      throw new PrecheckV2Exception(
          "INSUFFICIENT_ROLE", HttpStatus.FORBIDDEN, "当前身份无权执行该操作", Map.of("mockData", true));
    }
  }

  private void requirePolicyReader(boolean allowed) {
    if (!allowed) {
      throw new PrecheckV2Exception(
          "INSUFFICIENT_ROLE", HttpStatus.FORBIDDEN, "当前身份无权执行该操作", Map.of("mockData", true));
    }
  }

  private PrecheckV2Exception validation(String field, String message) {
    return new PrecheckV2Exception(
        "VALIDATION_ERROR",
        HttpStatus.BAD_REQUEST,
        message,
        Map.of("field", field, "mockData", true));
  }

  private PrecheckV2Exception conflict(String code, String message, Map<String, Object> details) {
    return new PrecheckV2Exception(code, HttpStatus.CONFLICT, message, details);
  }

  private void record(
      String actor,
      String action,
      String subjectType,
      UUID subjectId,
      String productLineCode,
      Map<String, Object> metadata,
      java.time.Instant occurredAt) {
    audit.record(
        new StoredAuditEvent(
            new StructuredAuditEvent(
                UUID.randomUUID(),
                actor,
                action,
                subjectType,
                subjectId,
                "SUCCEEDED",
                metadata,
                occurredAt,
                true),
            productLineCode,
            null));
  }
}
