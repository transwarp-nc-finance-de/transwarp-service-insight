package com.transwarp.serviceinsight.precheck.dto;

import java.util.List;

public record FollowUpResponse(
    String followUpId,
    String precheckId,
    String reply,
    List<String> recommendations,
    List<ReferenceItem> references,
    Confidence confidence,
    String confidenceReason,
    boolean humanReviewRequired,
    List<String> missingInformation,
    String fallbackReason,
    String nextAction,
    String nextActionReason,
    List<String> allowedActions,
    String status,
    String policyVersion,
    String modelVersion,
    String promptVersion,
    String indexVersion) {}
