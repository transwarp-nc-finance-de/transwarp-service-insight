package com.transwarp.serviceinsight.precheck.application;

public record CreatePrecheckCommand(
    String title,
    String description,
    String product,
    String module,
    String version,
    String severity,
    String impactScope,
    String attachmentsSummary) {}
