package com.transwarp.serviceinsight.precheck.application;

import com.transwarp.serviceinsight.audit.domain.AuditEvent;
import com.transwarp.serviceinsight.audit.domain.BusinessEventType;
import com.transwarp.serviceinsight.audit.port.AuditPort;
import com.transwarp.serviceinsight.policy.domain.PolicySnapshot;
import com.transwarp.serviceinsight.policy.port.PolicyContextPort;
import com.transwarp.serviceinsight.precheck.domain.*;
import com.transwarp.serviceinsight.precheck.port.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PrecheckWorkflowService
    implements CreatePrecheckUseCase,
        ContinuePrecheckUseCase,
        ContinueSessionPrecheckUseCase,
        GetPrecheckSessionUseCase {
  private static final String FALLBACK = "模拟数据：使用确定性 Mock 规则，未调用真实数据库、RAG、大模型或外部服务。";
  private final PrecheckSessionRepository sessions;
  private final PolicyContextPort policies;
  private final CompletenessAssessmentPort completeness;
  private final KnowledgeSearchPort knowledge;
  private final SuggestionGenerationPort suggestions;
  private final EvidenceVerificationPort verification;
  private final AuditPort audit;

  public PrecheckWorkflowService(
      PrecheckSessionRepository sessions,
      PolicyContextPort policies,
      CompletenessAssessmentPort completeness,
      KnowledgeSearchPort knowledge,
      SuggestionGenerationPort suggestions,
      EvidenceVerificationPort verification,
      AuditPort audit) {
    this.sessions = sessions;
    this.policies = policies;
    this.completeness = completeness;
    this.knowledge = knowledge;
    this.suggestions = suggestions;
    this.verification = verification;
    this.audit = audit;
  }

  @Override
  public PrecheckResult create(CreatePrecheckCommand command) {
    var policy = policies.currentSnapshot();
    var precheckId = UUID.randomUUID();
    var session = new PrecheckSession(UUID.randomUUID(), precheckId, command.hostRequestId());
    var run =
        session.startRun(
            "标题长度="
                + command.title().length()
                + ", 描述长度="
                + command.description().length()
                + "（正文不持久化）");
    record(precheckId, BusinessEventType.PRECHECK_STARTED, policy, Map.of("runSequence", "1"));
    var missing = completeness.missingInformation(command);
    var status =
        missing.isEmpty() ? PrecheckStatus.COMPLETED : PrecheckStatus.NEED_MORE_INFORMATION;
    var evidence =
        verification.verify(knowledge.search(command.title() + " " + command.description()));
    var result =
        new PrecheckResult(
            precheckId,
            session.id(),
            run.id(),
            run.sequence(),
            "模拟预诊：已整理辅助排查方向；这不是最终根因或处理结论。",
            suggestions.generate(command, missing),
            evidence,
            missing.size() >= 4 ? ConfidenceLevel.LOW : ConfidenceLevel.MEDIUM,
            missing.isEmpty() ? "模拟字段较完整，但仍无真实证据或模型判断。" : "仍缺少 " + missing.size() + " 项可核验信息。",
            true,
            missing,
            FALLBACK,
            missing.isEmpty()
                ? NextAction.MANUAL_REVIEW_REQUIRED
                : NextAction.NEED_MORE_INFORMATION,
            missing.isEmpty() ? "仍需人工核对后决定后续动作。" : "仍有信息需要人工补充。",
            List.of(AllowedAction.SUPPLEMENT_INFORMATION, AllowedAction.CONTINUE_SUBMISSION),
            status,
            policy.version(),
            "not-applicable-deterministic-mock",
            "mock-rule-v1",
            "not-applicable-no-index",
            new ExecutionMetadata(
                policy.version(),
                "mock-rule-v1",
                "not-applicable-deterministic-mock",
                "not-applicable-no-index"),
            Degradation.mock(FALLBACK));
    session.completeRun(run.id(), status);
    sessions.save(session);
    record(
        precheckId, BusinessEventType.PRECHECK_COMPLETED, policy, Map.of("status", status.name()));
    return result;
  }

  @Override
  public FollowUpResult continuePrecheck(ContinuePrecheckCommand command) {
    var policy = policies.currentSnapshot();
    var session =
        sessions
            .findByPrecheckId(command.precheckId())
            .orElseThrow(() -> new PrecheckSessionNotFoundException(command.precheckId()));
    var run = session.startRun("补充信息（正文不持久化）");
    var suggestion = suggestions.followUp(command.message());
    var status =
        suggestion.matched() ? PrecheckStatus.COMPLETED : PrecheckStatus.NEED_MORE_INFORMATION;
    var evidence = verification.verify(knowledge.search(suggestion.evidenceTitle()));
    var result =
        new FollowUpResult(
            UUID.randomUUID(),
            command.precheckId(),
            session.id(),
            run.id(),
            run.sequence(),
            suggestion.reply(),
            suggestion.recommendations(),
            evidence,
            suggestion.matched() ? ConfidenceLevel.MEDIUM : ConfidenceLevel.LOW,
            suggestion.matched() ? "仅命中确定性关键词规则，未经过真实证据核验。" : "补充内容未命中可核验的模拟分类规则。",
            true,
            suggestion.missingInformation(),
            FALLBACK,
            suggestion.matched()
                ? NextAction.MANUAL_REVIEW_REQUIRED
                : NextAction.NEED_MORE_INFORMATION,
            "始终允许人工继续提交。",
            List.of(AllowedAction.SUPPLEMENT_INFORMATION, AllowedAction.CONTINUE_SUBMISSION),
            status,
            policy.version(),
            "not-applicable-deterministic-mock",
            "mock-rule-v1",
            "not-applicable-no-index",
            new ExecutionMetadata(
                policy.version(),
                "mock-rule-v1",
                "not-applicable-deterministic-mock",
                "not-applicable-no-index"),
            Degradation.mock(FALLBACK));
    session.completeRun(run.id(), status);
    sessions.save(session);
    record(
        command.precheckId(),
        BusinessEventType.INFORMATION_ADDED,
        policy,
        Map.of("runSequence", Integer.toString(run.sequence()), "status", status.name()));
    return result;
  }

  @Override
  public FollowUpResult continueSession(UUID sessionId, String message) {
    var session =
        sessions
            .findById(sessionId)
            .orElseThrow(() -> new PrecheckSessionIdNotFoundException(sessionId));
    return continuePrecheck(new ContinuePrecheckCommand(session.precheckId(), message));
  }

  @Override
  public PrecheckSession getSession(UUID sessionId) {
    return sessions
        .findById(sessionId)
        .orElseThrow(() -> new PrecheckSessionIdNotFoundException(sessionId));
  }

  private void record(
      UUID precheckId,
      BusinessEventType type,
      PolicySnapshot policy,
      Map<String, String> metadata) {
    audit.record(
        new AuditEvent(
            UUID.randomUUID(),
            type,
            precheckId,
            policy.subjectId(),
            policy.version(),
            Instant.now(),
            metadata));
  }
}
