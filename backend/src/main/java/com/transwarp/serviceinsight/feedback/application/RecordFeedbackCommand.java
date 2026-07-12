package com.transwarp.serviceinsight.feedback.application;

import com.transwarp.serviceinsight.feedback.domain.AdoptionStatus;
import java.util.UUID;

public record RecordFeedbackCommand(
    UUID precheckId,
    AdoptionStatus adoptionStatus,
    String feedbackReason,
    boolean continuedSubmission) {}
