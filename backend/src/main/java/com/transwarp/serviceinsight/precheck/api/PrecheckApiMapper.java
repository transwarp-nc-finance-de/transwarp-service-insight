package com.transwarp.serviceinsight.precheck.api;

import com.transwarp.serviceinsight.precheck.application.ContinuePrecheckCommand;
import com.transwarp.serviceinsight.precheck.application.CreatePrecheckCommand;
import com.transwarp.serviceinsight.precheck.domain.FollowUpResult;
import com.transwarp.serviceinsight.precheck.domain.PrecheckResult;
import com.transwarp.serviceinsight.precheck.domain.PrecheckSession;
import com.transwarp.serviceinsight.precheck.dto.*;
import org.springframework.stereotype.Component;

@Component
public class PrecheckApiMapper {
  public PrecheckSessionResponse toResponse(PrecheckSession session) {
    return new PrecheckSessionResponse(
        session.id().toString(),
        session.precheckId().toString(),
        session.status().name(),
        session.runs().stream()
            .map(
                run ->
                    new RunSummaryResponse(
                        run.id().toString(),
                        run.sequence(),
                        run.status().name(),
                        run.inputSummary()))
            .toList());
  }

  public CreatePrecheckCommand toCommand(PrecheckRequest r) {
    return new CreatePrecheckCommand(
        r.title(),
        r.description(),
        r.product(),
        r.module(),
        r.version(),
        r.severity(),
        r.impactScope(),
        r.attachmentsSummary(),
        r.context() == null || r.context().sourceSystem() == null
            ? "SANDBOX"
            : r.context().sourceSystem(),
        r.context() == null ? null : r.context().hostRequestId(),
        r.context() == null || r.context().formSchemaVersion() == null
            ? "v1"
            : r.context().formSchemaVersion());
  }

  public ContinuePrecheckCommand toCommand(FollowUpRequest r) {
    return new ContinuePrecheckCommand(r.precheckId(), r.message());
  }

  public PrecheckResponse toResponse(PrecheckResult r) {
    return new PrecheckResponse(
        r.precheckId().toString(),
        r.sessionId().toString(),
        r.runId().toString(),
        r.runSequence(),
        r.summary(),
        r.recommendations(),
        r.evidence().stream()
            .map(
                e ->
                    new ReferenceItem(
                        ReferenceSourceType.valueOf(e.sourceType().name()),
                        e.title(),
                        e.excerpt(),
                        e.url(),
                        e.mockData()))
            .toList(),
        Confidence.valueOf(r.confidence().name()),
        r.confidenceReason(),
        r.humanReviewRequired(),
        r.missingInformation(),
        r.fallbackReason(),
        r.nextAction().name(),
        r.nextActionReason(),
        r.allowedActions().stream().map(Enum::name).toList(),
        r.status().name(),
        r.policyVersion(),
        r.modelVersion(),
        r.promptVersion(),
        r.indexVersion(),
        new ExecutionMetadataResponse(
            r.executionMetadata().policyVersion(),
            r.executionMetadata().promptVersion(),
            r.executionMetadata().modelVersion(),
            r.executionMetadata().indexVersion()),
        new DegradationResponse(
            r.degradation().degraded(), r.degradation().code(), r.degradation().message()));
  }

  public FollowUpResponse toResponse(FollowUpResult r) {
    return new FollowUpResponse(
        r.followUpId().toString(),
        r.precheckId().toString(),
        r.sessionId().toString(),
        r.runId().toString(),
        r.runSequence(),
        r.reply(),
        r.recommendations(),
        r.evidence().stream()
            .map(
                e ->
                    new ReferenceItem(
                        ReferenceSourceType.valueOf(e.sourceType().name()),
                        e.title(),
                        e.excerpt(),
                        e.url(),
                        e.mockData()))
            .toList(),
        Confidence.valueOf(r.confidence().name()),
        r.confidenceReason(),
        r.humanReviewRequired(),
        r.missingInformation(),
        r.fallbackReason(),
        r.nextAction().name(),
        r.nextActionReason(),
        r.allowedActions().stream().map(Enum::name).toList(),
        r.status().name(),
        r.policyVersion(),
        r.modelVersion(),
        r.promptVersion(),
        r.indexVersion(),
        new ExecutionMetadataResponse(
            r.executionMetadata().policyVersion(),
            r.executionMetadata().promptVersion(),
            r.executionMetadata().modelVersion(),
            r.executionMetadata().indexVersion()),
        new DegradationResponse(
            r.degradation().degraded(), r.degradation().code(), r.degradation().message()));
  }
}
