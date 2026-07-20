package com.transwarp.serviceinsight.precheck.interaction.application;

import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StoredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.domain.StructuredAuditModels.StructuredAuditEvent;
import com.transwarp.serviceinsight.audit.v2.port.StructuredAuditPort;
import com.transwarp.serviceinsight.identity.application.AuthSessionApplicationService;
import com.transwarp.serviceinsight.identity.domain.IdentityContext;
import com.transwarp.serviceinsight.identity.domain.Role;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.ContinuationCreationResult;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.ContinuationRecord;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.Feedback;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.FeedbackCreationResult;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.FeedbackRecord;
import com.transwarp.serviceinsight.precheck.interaction.domain.InteractionModels.SubmissionContinuation;
import com.transwarp.serviceinsight.precheck.interaction.port.InteractionRepository;
import com.transwarp.serviceinsight.precheck.v2.application.PrecheckContextNormalizer;
import com.transwarp.serviceinsight.precheck.v2.application.PrecheckV2Exception;
import com.transwarp.serviceinsight.precheck.v2.domain.PersistentPrecheckModels.PrecheckSession;
import com.transwarp.serviceinsight.precheck.v2.port.PersistentPrecheckRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InteractionService {
  private final InteractionRepository interactions;
  private final PersistentPrecheckRepository prechecks;
  private final AuthSessionApplicationService authSessions;
  private final PrecheckContextNormalizer normalizer;
  private final StructuredAuditPort audit;
  private final Clock clock;

  public InteractionService(
      InteractionRepository interactions,
      PersistentPrecheckRepository prechecks,
      AuthSessionApplicationService authSessions,
      PrecheckContextNormalizer normalizer,
      StructuredAuditPort audit,
      Clock clock) {
    this.interactions = interactions;
    this.prechecks = prechecks;
    this.authSessions = authSessions;
    this.normalizer = normalizer;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional
  public FeedbackCreationResult createFeedback(
      UUID sessionId,
      UUID runId,
      String adoptionStatus,
      String helpfulness,
      String reason,
      String idempotencyKey,
      String sessionCookie,
      String csrfToken) {
    validateIdempotencyKey(idempotencyKey);
    var auth = authSessions.requireWriteSession(sessionCookie, csrfToken);
    requirePrecheckUser(auth.identity());
    var requestHash =
        normalizer.feedbackCommandHash(sessionId, runId, adoptionStatus, helpfulness, reason);
    prechecks.lockCommand("FEEDBACK", idempotencyKey);
    var replay = interactions.findFeedbackIdempotency(idempotencyKey);
    if (replay.isPresent()) {
      if (!replay.get().requestHash().equals(requestHash)) throw idempotencyConflict();
      authorize(replay.get().record(), auth.identity());
      return new FeedbackCreationResult(replay.get().record().feedback(), true);
    }
    var session = prechecks.lockSession(sessionId).orElseThrow(this::notFound);
    authorize(session, auth.identity());
    if (!"ACTIVE".equals(session.status())) throw terminalSession();
    prechecks.findRun(sessionId, runId).orElseThrow(this::notFound);
    var feedback =
        new Feedback(
            UUID.randomUUID(),
            sessionId,
            runId,
            adoptionStatus,
            helpfulness,
            reason,
            clock.instant(),
            true);
    interactions.saveFeedback(
        new FeedbackRecord(
            feedback,
            auth.identity().userCode(),
            session.latestRun().contextSnapshot().productLine().code()),
        idempotencyKey,
        requestHash);
    var metadata = new LinkedHashMap<String, Object>();
    metadata.put("sessionId", sessionId.toString());
    metadata.put("runId", runId.toString());
    metadata.put("adoptionStatus", adoptionStatus);
    if (helpfulness != null) metadata.put("helpfulness", helpfulness);
    metadata.put("productLineCode", session.latestRun().contextSnapshot().productLine().code());
    metadata.put("degraded", session.latestRun().result().retrieval().degraded());
    audit.record(
        new StoredAuditEvent(
            new StructuredAuditEvent(
                UUID.randomUUID(),
                auth.identity().userCode(),
                "FEEDBACK_RECORDED",
                "Feedback",
                feedback.feedbackId(),
                "SUCCEEDED",
                metadata,
                feedback.recordedAt(),
                true),
            session.latestRun().contextSnapshot().productLine().code(),
            null));
    return new FeedbackCreationResult(feedback, false);
  }

  public Feedback getFeedback(UUID feedbackId, String sessionCookie) {
    var auth = authSessions.current(sessionCookie);
    requirePrecheckUser(auth.identity());
    var record = interactions.findFeedback(feedbackId).orElseThrow(this::notFound);
    authorize(record, auth.identity());
    return record.feedback();
  }

  @Transactional
  public ContinuationCreationResult createContinuation(
      UUID sessionId,
      boolean confirmed,
      String reason,
      String idempotencyKey,
      String sessionCookie,
      String csrfToken) {
    validateIdempotencyKey(idempotencyKey);
    var auth = authSessions.requireWriteSession(sessionCookie, csrfToken);
    requirePrecheckUser(auth.identity());
    var requestHash = normalizer.continuationCommandHash(sessionId, confirmed, reason);
    prechecks.lockCommand("CONTINUATION", idempotencyKey);
    var replay = interactions.findContinuationIdempotency(idempotencyKey);
    if (replay.isPresent()) {
      if (!replay.get().requestHash().equals(requestHash)) throw idempotencyConflict();
      authorize(replay.get().record(), auth.identity());
      return new ContinuationCreationResult(replay.get().record().continuation(), true);
    }
    var session = prechecks.lockSession(sessionId).orElseThrow(this::notFound);
    authorize(session, auth.identity());
    if (!"ACTIVE".equals(session.status())) throw terminalSession();
    var now = clock.instant();
    var continuation =
        new SubmissionContinuation(
            UUID.randomUUID(),
            sessionId,
            auth.identity().userCode(),
            now,
            reason,
            UUID.randomUUID(),
            true);
    interactions.saveContinuation(
        new ContinuationRecord(
            continuation, session.latestRun().contextSnapshot().productLine().code()),
        idempotencyKey,
        requestHash);
    audit.record(
        new StoredAuditEvent(
            new StructuredAuditEvent(
                continuation.auditEventId(),
                auth.identity().userCode(),
                "SUBMISSION_CONTINUED",
                "SubmissionContinuation",
                continuation.continuationId(),
                "SUCCEEDED",
                Map.of(
                    "sessionId",
                    sessionId.toString(),
                    "productLineCode",
                    session.latestRun().contextSnapshot().productLine().code(),
                    "terminationReason",
                    "CONTINUED_SUBMISSION",
                    "degraded",
                    session.latestRun().result().retrieval().degraded()),
                continuation.confirmedAt(),
                true),
            session.latestRun().contextSnapshot().productLine().code(),
            null));
    return new ContinuationCreationResult(continuation, false);
  }

  public SubmissionContinuation getContinuation(UUID continuationId, String sessionCookie) {
    var auth = authSessions.current(sessionCookie);
    requirePrecheckUser(auth.identity());
    var record = interactions.findContinuation(continuationId).orElseThrow(this::notFound);
    authorize(record, auth.identity());
    return record.continuation();
  }

  private void authorize(PrecheckSession session, IdentityContext identity) {
    if (!session.ownerUserCode().equals(identity.userCode())
        || !identity.canAccessProductLine(
            session.latestRun().contextSnapshot().productLine().code())) {
      throw notFound();
    }
  }

  private void authorize(FeedbackRecord record, IdentityContext identity) {
    if (!record.ownerUserCode().equals(identity.userCode())
        || !identity.canAccessProductLine(record.productLineCode())) {
      throw notFound();
    }
  }

  private void authorize(ContinuationRecord record, IdentityContext identity) {
    if (!record.continuation().confirmedBy().equals(identity.userCode())
        || !identity.canAccessProductLine(record.productLineCode())) {
      throw notFound();
    }
  }

  private void requirePrecheckUser(IdentityContext identity) {
    if (!identity.hasRole(Role.PRECHECK_USER)) {
      throw new PrecheckV2Exception(
          "INSUFFICIENT_ROLE", HttpStatus.FORBIDDEN, "当前身份无权执行该操作", Map.of("mockData", true));
    }
  }

  private void validateIdempotencyKey(String value) {
    if (value == null || value.length() < 8 || value.length() > 128) {
      throw new PrecheckV2Exception(
          "VALIDATION_ERROR",
          HttpStatus.BAD_REQUEST,
          "幂等键长度必须为 8 到 128",
          Map.of("field", "Idempotency-Key", "mockData", true));
    }
  }

  private PrecheckV2Exception notFound() {
    return new PrecheckV2Exception("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "资源不存在", Map.of());
  }

  private PrecheckV2Exception idempotencyConflict() {
    return new PrecheckV2Exception(
        "IDEMPOTENCY_KEY_CONFLICT", HttpStatus.CONFLICT, "幂等键已用于不同请求", Map.of("mockData", true));
  }

  private PrecheckV2Exception terminalSession() {
    return new PrecheckV2Exception(
        "ILLEGAL_STATE_TRANSITION", HttpStatus.CONFLICT, "终态 Session 只读", Map.of("mockData", true));
  }
}
