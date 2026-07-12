package com.transwarp.serviceinsight.precheck.application;

import com.transwarp.serviceinsight.audit.domain.AuditEvent;
import com.transwarp.serviceinsight.audit.domain.BusinessEventType;
import com.transwarp.serviceinsight.audit.port.AuditPort;
import com.transwarp.serviceinsight.policy.domain.PolicySnapshot;
import com.transwarp.serviceinsight.policy.port.PolicyContextPort;
import com.transwarp.serviceinsight.precheck.domain.FollowUpResult;
import com.transwarp.serviceinsight.precheck.domain.PrecheckResult;
import com.transwarp.serviceinsight.precheck.port.PrecheckExecutionPort;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PrecheckApplicationService implements CreatePrecheckUseCase, ContinuePrecheckUseCase {
  private final PrecheckExecutionPort executionPort;
  private final PolicyContextPort policyContext;
  private final AuditPort audit;

  public PrecheckApplicationService(
      PrecheckExecutionPort executionPort, PolicyContextPort policyContext, AuditPort audit) {
    this.executionPort = executionPort;
    this.policyContext = policyContext;
    this.audit = audit;
  }

  @Override
  public PrecheckResult create(CreatePrecheckCommand command) {
    var policy = policyContext.currentSnapshot();
    audit(null, BusinessEventType.PRECHECK_STARTED, policy, Map.of("mockData", "true"));
    var result = withPolicy(executionPort.create(command), policy.version());
    audit(
        result.precheckId(),
        BusinessEventType.PRECHECK_COMPLETED,
        policy,
        Map.of("confidence", result.confidence().name(), "status", result.status().name()));
    return result;
  }

  @Override
  public FollowUpResult continuePrecheck(ContinuePrecheckCommand command) {
    var policy = policyContext.currentSnapshot();
    var result = withPolicy(executionPort.continuePrecheck(command), policy.version());
    audit(
        command.precheckId(),
        BusinessEventType.INFORMATION_ADDED,
        policy,
        Map.of("confidence", result.confidence().name(), "status", result.status().name()));
    return result;
  }

  private PrecheckResult withPolicy(PrecheckResult result, String version) {
    return new PrecheckResult(
        result.precheckId(),
        result.sessionId(),
        result.summary(),
        result.recommendations(),
        result.evidence(),
        result.confidence(),
        result.confidenceReason(),
        result.humanReviewRequired(),
        result.missingInformation(),
        result.fallbackReason(),
        result.nextAction(),
        result.nextActionReason(),
        result.allowedActions(),
        result.status(),
        version,
        result.modelVersion(),
        result.promptVersion(),
        result.indexVersion());
  }

  private FollowUpResult withPolicy(FollowUpResult result, String version) {
    return new FollowUpResult(
        result.followUpId(),
        result.precheckId(),
        result.reply(),
        result.recommendations(),
        result.evidence(),
        result.confidence(),
        result.confidenceReason(),
        result.humanReviewRequired(),
        result.missingInformation(),
        result.fallbackReason(),
        result.nextAction(),
        result.nextActionReason(),
        result.allowedActions(),
        result.status(),
        version,
        result.modelVersion(),
        result.promptVersion(),
        result.indexVersion());
  }

  private void audit(
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
