package com.transwarp.serviceinsight.precheck.application;

public record CreatePrecheckCommand(
    String title,
    String description,
    String product,
    String module,
    String version,
    String severity,
    String impactScope,
    String attachmentsSummary,
    String sourceSystem,
    String hostRequestId,
    String formSchemaVersion) {
  public CreatePrecheckCommand(
      String title,
      String description,
      String product,
      String module,
      String version,
      String severity,
      String impactScope,
      String attachmentsSummary) {
    this(
        title,
        description,
        product,
        module,
        version,
        severity,
        impactScope,
        attachmentsSummary,
        "SANDBOX",
        null,
        "v1");
  }
}
