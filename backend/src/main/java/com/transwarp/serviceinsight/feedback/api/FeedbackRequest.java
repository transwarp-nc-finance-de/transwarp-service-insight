package com.transwarp.serviceinsight.feedback.api;

import com.transwarp.serviceinsight.feedback.domain.AdoptionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FeedbackRequest(
    @NotNull(message = "precheckId 不能为空") UUID precheckId,
    @NotNull(message = "adoptionStatus 不能为空") AdoptionStatus adoptionStatus,
    @Size(max = 2000, message = "feedbackReason 长度不能超过 2000 个字符") String feedbackReason,
    boolean continuedSubmission) {}
