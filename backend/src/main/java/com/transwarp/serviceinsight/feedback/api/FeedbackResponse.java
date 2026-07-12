package com.transwarp.serviceinsight.feedback.api;

public record FeedbackResponse(
    String feedbackId,
    String precheckId,
    String adoptionStatus,
    boolean continuedSubmission,
    String policyVersion,
    boolean recorded,
    boolean mockData) {}
