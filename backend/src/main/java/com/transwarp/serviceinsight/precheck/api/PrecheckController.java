package com.transwarp.serviceinsight.precheck.api;

import com.transwarp.serviceinsight.precheck.application.ContinuePrecheckCommand;
import com.transwarp.serviceinsight.precheck.application.ContinuePrecheckUseCase;
import com.transwarp.serviceinsight.precheck.application.CreatePrecheckCommand;
import com.transwarp.serviceinsight.precheck.application.CreatePrecheckUseCase;
import com.transwarp.serviceinsight.precheck.dto.FollowUpRequest;
import com.transwarp.serviceinsight.precheck.dto.FollowUpResponse;
import com.transwarp.serviceinsight.precheck.dto.PrecheckRequest;
import com.transwarp.serviceinsight.precheck.dto.PrecheckResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PrecheckController implements PrecheckApi {
  private final CreatePrecheckUseCase createPrecheck;
  private final ContinuePrecheckUseCase continuePrecheck;

  public PrecheckController(
      CreatePrecheckUseCase createPrecheck, ContinuePrecheckUseCase continuePrecheck) {
    this.createPrecheck = createPrecheck;
    this.continuePrecheck = continuePrecheck;
  }

  @Override
  public PrecheckResponse precheck(PrecheckRequest request) {
    var result =
        createPrecheck.create(
            new CreatePrecheckCommand(
                request.title(),
                request.description(),
                request.product(),
                request.module(),
                request.version(),
                request.severity(),
                request.impactScope(),
                request.attachmentsSummary()));
    return new PrecheckResponse(
        result.precheckId().toString(),
        result.sessionId().toString(),
        result.summary(),
        result.recommendations(),
        result.evidence().stream()
            .map(
                evidence ->
                    new com.transwarp.serviceinsight.precheck.dto.ReferenceItem(
                        com.transwarp.serviceinsight.precheck.dto.ReferenceSourceType.valueOf(
                            evidence.sourceType().name()),
                        evidence.title(),
                        evidence.excerpt(),
                        evidence.url(),
                        evidence.mockData()))
            .toList(),
        com.transwarp.serviceinsight.precheck.dto.Confidence.valueOf(result.confidence().name()),
        result.confidenceReason(),
        result.humanReviewRequired(),
        result.missingInformation(),
        result.fallbackReason(),
        result.nextAction().name(),
        result.nextActionReason(),
        result.allowedActions().stream().map(Enum::name).toList(),
        result.status().name(),
        result.policyVersion(),
        result.modelVersion(),
        result.promptVersion(),
        result.indexVersion());
  }

  @Override
  public FollowUpResponse followUp(FollowUpRequest request) {
    var result =
        continuePrecheck.continuePrecheck(
            new ContinuePrecheckCommand(request.precheckId(), request.message()));
    return new FollowUpResponse(
        result.followUpId().toString(),
        result.precheckId().toString(),
        result.reply(),
        result.recommendations(),
        result.evidence().stream()
            .map(
                evidence ->
                    new com.transwarp.serviceinsight.precheck.dto.ReferenceItem(
                        com.transwarp.serviceinsight.precheck.dto.ReferenceSourceType.valueOf(
                            evidence.sourceType().name()),
                        evidence.title(),
                        evidence.excerpt(),
                        evidence.url(),
                        evidence.mockData()))
            .toList(),
        com.transwarp.serviceinsight.precheck.dto.Confidence.valueOf(result.confidence().name()),
        result.confidenceReason(),
        result.humanReviewRequired(),
        result.missingInformation(),
        result.fallbackReason(),
        result.nextAction().name(),
        result.nextActionReason(),
        result.allowedActions().stream().map(Enum::name).toList(),
        result.status().name(),
        result.policyVersion(),
        result.modelVersion(),
        result.promptVersion(),
        result.indexVersion());
  }
}
