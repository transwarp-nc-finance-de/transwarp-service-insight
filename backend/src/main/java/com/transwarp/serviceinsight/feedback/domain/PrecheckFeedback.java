package com.transwarp.serviceinsight.feedback.domain;

import java.time.Instant;
import java.util.UUID;

public record PrecheckFeedback(
    UUID id,
    UUID precheckId,
    AdoptionStatus adoptionStatus,
    String feedbackReason,
    boolean continuedSubmission,
    String policyVersion,
    Instant recordedAt) {}
