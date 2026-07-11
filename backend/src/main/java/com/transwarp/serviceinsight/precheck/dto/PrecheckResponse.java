package com.transwarp.serviceinsight.precheck.dto;

import java.util.List;

public record PrecheckResponse(
    String precheckId,
    String summary,
    List<String> recommendations,
    List<ReferenceItem> references,
    Confidence confidence,
    boolean humanReviewRequired,
    List<String> missingInformation,
    String fallbackReason) {}
