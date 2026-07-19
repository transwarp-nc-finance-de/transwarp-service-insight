package com.transwarp.serviceinsight.knowledge.governance.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transwarp.serviceinsight.identity.api.V2FieldError;
import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.identity.application.CsrfValidationFailedException;
import com.transwarp.serviceinsight.identity.domain.IdentityContext;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.knowledge.governance.api.KnowledgeApprovalRequest;
import com.transwarp.serviceinsight.knowledge.governance.api.KnowledgeRevisionRequest;
import com.transwarp.serviceinsight.knowledge.governance.api.RequiredCommandReasonRequest;
import com.transwarp.serviceinsight.knowledge.governance.api.ReviewSubmissionRequest;
import com.transwarp.serviceinsight.knowledge.governance.domain.DutySeparationViolation;
import com.transwarp.serviceinsight.knowledge.governance.domain.IllegalGovernanceTransition;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.CommandResult;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.GovernanceCommand;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.NewRevision;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.RevisionCreated;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernanceModels.VersionState;
import com.transwarp.serviceinsight.knowledge.governance.domain.KnowledgeGovernancePolicy;
import com.transwarp.serviceinsight.knowledge.governance.port.KnowledgeGovernanceRepository;
import com.transwarp.serviceinsight.knowledge.ingestion.application.KnowledgeApiException;
import com.transwarp.serviceinsight.knowledge.ingestion.application.KnowledgeParseProcessor;
import com.transwarp.serviceinsight.knowledge.ingestion.domain.KnowledgeIngestionModels.CatalogValue;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class KnowledgeGovernanceService {
  private final AuthSessionApplicationService authSessions;
  private final KnowledgeGovernanceRepository repository;
  private final KnowledgeParseProcessor parseProcessor;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public KnowledgeGovernanceService(
      AuthSessionApplicationService authSessions,
      KnowledgeGovernanceRepository repository,
      KnowledgeParseProcessor parseProcessor,
      ObjectMapper objectMapper,
      Clock clock) {
    this.authSessions = authSessions;
    this.repository = repository;
    this.parseProcessor = parseProcessor;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional
  public RevisionResponse revise(
      String sessionCookie,
      String csrfToken,
      String idempotencyKey,
      UUID versionId,
      KnowledgeRevisionRequest request) {
    var identity = authorize(sessionCookie, csrfToken, Role.KNOWLEDGE_EDITOR);
    validateIdempotencyKey(idempotencyKey);
    repository.lockIdempotency("REVISION", idempotencyKey);
    var state = visibleEditorVersion(versionId, identity);
    var requestHash =
        requestHash(
            "REVISION",
            versionId,
            identity.userCode(),
            request.title(),
            request.productLine(),
            request.cleanedText(),
            request.parseWarningNotes());
    var replay = replayRevision("REVISION", idempotencyKey, requestHash);
    if (replay != null) return replay;
    if (!"DRAFT".equals(state.version().status())) {
      throw illegalState("当前状态不允许创建草稿修订");
    }
    if (!identity.canAccessProductLine(request.productLine().code())) {
      throw notFound();
    }
    var revisionId = UUID.randomUUID();
    var taskId = UUID.randomUUID();
    var created =
        repository.createRevision(
            new NewRevision(
                revisionId,
                taskId,
                versionId,
                state.currentDraftRevisionNumber() + 1,
                request.title(),
                new CatalogValue(request.productLine().code(), request.productLine().displayName()),
                request.cleanedText(),
                sha256(request.cleanedText().getBytes(StandardCharsets.UTF_8)),
                request.parseWarningNotes(),
                identity.userCode(),
                idempotencyKey,
                requestHash,
                clock.instant()));
    enqueueAfterCommit(taskId);
    return new RevisionResponse(created, false);
  }

  @Transactional
  public CommandResponse submit(
      String sessionCookie,
      String csrfToken,
      String idempotencyKey,
      UUID versionId,
      ReviewSubmissionRequest request) {
    var identity = authorize(sessionCookie, csrfToken, Role.KNOWLEDGE_EDITOR);
    validateIdempotencyKey(idempotencyKey);
    repository.lockIdempotency("SUBMIT", idempotencyKey);
    var state = visibleEditorVersion(versionId, identity);
    var requestHash =
        requestHash(
            "SUBMIT", versionId, identity.userCode(), request.parseResultHash(), request.reason());
    var replay = replayCommand("SUBMIT", idempotencyKey, requestHash);
    if (replay != null) return replay;
    try {
      var target = KnowledgeGovernancePolicy.submit(state.version().status());
      requireSucceededParse(state);
      KnowledgeGovernancePolicy.validateCurrentParseHash(
          request.parseResultHash(), state.parseResultHash());
      return apply(
          "SUBMIT",
          target,
          state,
          identity,
          request.parseResultHash(),
          Set.of(),
          request.reason(),
          idempotencyKey,
          requestHash,
          identity.userCode(),
          null);
    } catch (IllegalGovernanceTransition exception) {
      throw illegalState(exception.getMessage());
    }
  }

  @Transactional
  public CommandResponse returnToDraft(
      String sessionCookie,
      String csrfToken,
      String idempotencyKey,
      UUID versionId,
      RequiredCommandReasonRequest request) {
    var identity = authorize(sessionCookie, csrfToken, Role.KNOWLEDGE_REVIEWER);
    validateIdempotencyKey(idempotencyKey);
    repository.lockIdempotency("RETURN", idempotencyKey);
    var state = visibleReviewerVersion(versionId, identity);
    var requestHash = requestHash("RETURN", versionId, identity.userCode(), request.reason());
    var replay = replayCommand("RETURN", idempotencyKey, requestHash);
    if (replay != null) return replay;
    try {
      return apply(
          "RETURN",
          KnowledgeGovernancePolicy.returnToDraft(state.version().status()),
          state,
          identity,
          null,
          Set.of(),
          request.reason(),
          idempotencyKey,
          requestHash,
          state.version().submittedBy(),
          null);
    } catch (IllegalGovernanceTransition exception) {
      throw illegalState(exception.getMessage());
    }
  }

  @Transactional
  public CommandResponse approve(
      String sessionCookie,
      String csrfToken,
      String idempotencyKey,
      UUID versionId,
      KnowledgeApprovalRequest request) {
    var identity = authorize(sessionCookie, csrfToken, Role.KNOWLEDGE_REVIEWER);
    validateIdempotencyKey(idempotencyKey);
    repository.lockIdempotency("APPROVE", idempotencyKey);
    var state = visibleReviewerVersion(versionId, identity);
    var requestHash =
        requestHash(
            "APPROVE",
            versionId,
            identity.userCode(),
            request.parseResultHash(),
            request.acknowledgedWarningCodes(),
            request.reason());
    var replay = replayCommand("APPROVE", idempotencyKey, requestHash);
    if (replay != null) return replay;
    try {
      var target = KnowledgeGovernancePolicy.approve(state.version().status());
      requireSucceededParse(state);
      KnowledgeGovernancePolicy.validateApproval(
          state.version().submittedBy(),
          identity.userCode(),
          request.parseResultHash(),
          state.parseResultHash(),
          state.warningCodes(),
          request.acknowledgedWarningCodes());
      return apply(
          "APPROVE",
          target,
          state,
          identity,
          request.parseResultHash(),
          request.acknowledgedWarningCodes(),
          request.reason(),
          idempotencyKey,
          requestHash,
          state.version().submittedBy(),
          identity.userCode());
    } catch (DutySeparationViolation exception) {
      throw error(HttpStatus.FORBIDDEN, "DUTY_SEPARATION_VIOLATION", "提交人与审核人必须是不同身份");
    } catch (IllegalGovernanceTransition exception) {
      throw illegalState(exception.getMessage());
    }
  }

  @Transactional
  public CommandResponse deprecate(
      String sessionCookie,
      String csrfToken,
      String idempotencyKey,
      UUID versionId,
      RequiredCommandReasonRequest request) {
    var identity = authorize(sessionCookie, csrfToken, Role.KNOWLEDGE_REVIEWER);
    validateIdempotencyKey(idempotencyKey);
    repository.lockIdempotency("DEPRECATE", idempotencyKey);
    var state = visibleReviewerVersion(versionId, identity);
    var requestHash = requestHash("DEPRECATE", versionId, identity.userCode(), request.reason());
    var replay = replayCommand("DEPRECATE", idempotencyKey, requestHash);
    if (replay != null) return replay;
    if (!"PUBLISHED".equals(state.version().status())) {
      throw illegalState("仅 PUBLISHED 知识版本可废弃");
    }
    return apply(
        "DEPRECATE",
        "DEPRECATED",
        state,
        identity,
        null,
        Set.of(),
        request.reason(),
        idempotencyKey,
        requestHash,
        state.version().submittedBy(),
        state.version().approvedBy());
  }

  private CommandResponse apply(
      String commandType,
      String target,
      VersionState state,
      IdentityContext identity,
      String parseHash,
      Set<String> warningCodes,
      String reason,
      String idempotencyKey,
      String requestHash,
      String submittedBy,
      String approvedBy) {
    var result =
        repository.apply(
            new GovernanceCommand(
                commandType,
                state.version().versionId(),
                state.currentDraftRevisionId(),
                identity.userCode(),
                target,
                submittedBy,
                approvedBy,
                parseHash,
                warningCodes,
                reason,
                idempotencyKey,
                requestHash,
                UUID.randomUUID(),
                clock.instant()));
    return new CommandResponse(result, false);
  }

  private VersionState visibleEditorVersion(UUID versionId, IdentityContext identity) {
    var state = repository.lockVersion(versionId).orElseThrow(this::notFound);
    if (!identity.canAccessProductLine(state.productLineCode())
        || !identity.userCode().equals(state.createdBy())) {
      throw notFound();
    }
    return state;
  }

  private VersionState visibleReviewerVersion(UUID versionId, IdentityContext identity) {
    var state = repository.lockVersion(versionId).orElseThrow(this::notFound);
    if (!identity.canAccessProductLine(state.productLineCode())) throw notFound();
    return state;
  }

  private IdentityContext authorize(String sessionCookie, String csrfToken, Role requiredRole) {
    var session = authSessions.current(sessionCookie);
    requireCsrf(session.csrfToken(), csrfToken);
    if (!session.identity().hasRole(requiredRole)) {
      throw error(HttpStatus.FORBIDDEN, "INSUFFICIENT_ROLE", "当前身份无权执行该操作");
    }
    return session.identity();
  }

  private void requireSucceededParse(VersionState state) {
    if (!"SUCCEEDED".equals(state.parseStatus()) || state.parseResultHash() == null) {
      throw new IllegalGovernanceTransition("当前草稿修订尚未完成解析");
    }
  }

  private RevisionResponse replayRevision(String type, String key, String requestHash) {
    var existing = repository.findIdempotency(type, key);
    if (existing.isEmpty()) return null;
    if (!existing.get().requestHash().equals(requestHash)) throw idempotencyConflict();
    return new RevisionResponse(existing.get().revisionResult(), true);
  }

  private CommandResponse replayCommand(String type, String key, String requestHash) {
    var existing = repository.findIdempotency(type, key);
    if (existing.isEmpty()) return null;
    if (!existing.get().requestHash().equals(requestHash)) throw idempotencyConflict();
    return new CommandResponse(existing.get().commandResult(), true);
  }

  private void validateIdempotencyKey(String value) {
    if (value == null || value.length() < 8 || value.length() > 128) {
      throw new KnowledgeApiException(
          HttpStatus.BAD_REQUEST,
          "VALIDATION_ERROR",
          "请求字段校验失败",
          List.of(new V2FieldError("Idempotency-Key", "INVALID", "幂等键长度必须为 8 到 128")),
          false,
          Map.of("mockData", true));
    }
  }

  private void requireCsrf(String expected, String actual) {
    if (actual == null
        || actual.isBlank()
        || !MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
      throw new CsrfValidationFailedException();
    }
  }

  private String requestHash(Object... values) {
    try {
      return sha256(objectMapper.writeValueAsBytes(values));
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("无法规范化命令请求", exception);
    }
  }

  private String sha256(byte[] value) {
    try {
      return "sha256:"
          + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private void enqueueAfterCommit(UUID taskId) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            parseProcessor.enqueue(taskId);
          }
        });
  }

  private KnowledgeApiException illegalState(String message) {
    return error(HttpStatus.CONFLICT, "ILLEGAL_STATE_TRANSITION", message);
  }

  private KnowledgeApiException idempotencyConflict() {
    return error(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", "幂等键已用于不同请求");
  }

  private KnowledgeApiException notFound() {
    return new KnowledgeApiException(
        HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "资源不存在", List.of(), false, Map.of());
  }

  private KnowledgeApiException error(HttpStatus status, String code, String message) {
    return new KnowledgeApiException(
        status, code, message, List.of(), false, Map.of("mockData", true));
  }

  public record RevisionResponse(RevisionCreated body, boolean replayed) {}

  public record CommandResponse(CommandResult body, boolean replayed) {}
}
