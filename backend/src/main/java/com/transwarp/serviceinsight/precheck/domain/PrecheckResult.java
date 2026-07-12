package com.transwarp.serviceinsight.precheck.domain;

import java.util.List;
import java.util.UUID;

public record PrecheckResult(
    UUID precheckId,
    UUID sessionId,
    String summary,
    List<String> recommendations,
    List<Evidence> evidence,
    ConfidenceLevel confidence,
    String confidenceReason,
    boolean humanReviewRequired,
    List<String> missingInformation,
    String fallbackReason,
    NextAction nextAction,
    String nextActionReason,
    List<AllowedAction> allowedActions,
    PrecheckStatus status,
    String policyVersion,
    String modelVersion,
    String promptVersion,
    String indexVersion) {}
